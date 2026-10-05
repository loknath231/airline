package com.airline.service;

import com.airline.config.BookingWindow;
import com.airline.domain.*;
import com.airline.exception.ConflictException;
import com.airline.exception.InvalidRequestException;
import com.airline.exception.ResourceNotFoundException;
import com.airline.repository.BookingRepository;
import com.airline.repository.BookingSeatRepository;
import com.airline.repository.FlightInstanceRepository;
import com.airline.web.dto.BookingResponse;
import com.airline.web.dto.CreateBookingRequest;
import java.time.Clock;
import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.jspecify.annotations.NonNull;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Booking service is used for flight booking and cancelling
 * Concurrency strategy (defence in depth):
 *  1. Pessimistic lock (SELECT ... FOR UPDATE) on the flight_instance row serialises all
 *     booking/cancellation transactions of ONE flight;
 *     different flights run in parallel.
 *  2. UNIQUE(flight_instance_id, active_seat_label) is the final schema-level guarantee.
 *  3. Booking has @Version column to detect any unexpected concurrent modification.
 *
 * @author Loknath Kumar
 * @version 1.0
 * @since 2026-10-03
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class BookingService {
    private final FlightInstanceRepository flightInstanceRepository;
    private final BookingRepository bookingRepository;
    private final BookingSeatRepository bookingSeatRepository;
    private final BookingWindow bookingWindow;
    private final Clock clock;

    /**
     * Books the requested seats on one flight instance, all or nothing.
     *
     * @param createBookingRequest flight instance id and one seat per passenger
     * @return the confirmed booking
     */
    @Transactional
    public BookingResponse create(CreateBookingRequest createBookingRequest) {
        // 1. lock the flight row (concurrent bookers of the same flight wait until we commit)
        FlightInstance flightInstance = flightInstanceRepository
                .findByIdForUpdate(createBookingRequest.flightInstanceId())
                .orElseThrow(() ->
                        new ResourceNotFoundException("Flight not found: " + createBookingRequest.flightInstanceId()));
        Instant now = Instant.now(clock);
        if (flightInstance.getStatus() != FlightStatus.SCHEDULED)
            throw new ConflictException("Flight is not open for booking");
        if (!flightInstance.getDepartureUtc().isAfter(now)) throw new ConflictException("Flight has already departed");
        bookingWindow.validate(flightInstance.getFlightDate());
        Map<String, String> seatToPassenger = getSeatToPassenger(createBookingRequest, flightInstance);

        // 3. availability check (safe: we hold the flight lock)
        List<String> taken = bookingSeatRepository.findActiveSeatLabels(flightInstance.getId()).stream()
                .filter(seatToPassenger::containsKey)
                .sorted()
                .toList();
        if (!taken.isEmpty()) throw new ConflictException("Seats already booked: " + taken);

        // 4. persist booking + seats atomically
        Booking booking = new Booking();
        booking.setPnr(UUID.randomUUID().toString());
        booking.setFlightInstance(flightInstance);
        booking.setStatus(BookingStatus.CONFIRMED);
        booking.setPassengerCount(seatToPassenger.size());
        booking.setCreatedAt(now);
        seatToPassenger.forEach(booking::addSeat);
        try {
            bookingRepository.saveAndFlush(booking);
        } catch (DataIntegrityViolationException e) {
            throw new ConflictException("One or more seats were just booked by another customer");
        }
        log.info(
                "Booking {} created: flight={} date={} seats={}",
                booking.getPnr(),
                flightInstance.getSchedule().getFlightNumber(),
                flightInstance.getFlightDate(),
                seatToPassenger.keySet());
        return BookingResponse.from(booking);
    }

    /**
     * Normalises the requested seats and validates them against the aircraft layout.
     *
     * @param createBookingRequest booking request
     * @param flightInstance flight being booked
     * @return seat label -> passenger name, in request order
     */
    private static @NonNull Map<String, String> getSeatToPassenger(
            CreateBookingRequest createBookingRequest, FlightInstance flightInstance) {
        // 2. validate seats against the aircraft layout; reject duplicates inside the request
        Aircraft aircraft = flightInstance.getSchedule().getAircraft();
        Map<String, String> seatToPassenger = new LinkedHashMap<>();
        for (var passenger : createBookingRequest.passengers()) {
            String seat = passenger.seat().trim().toUpperCase();
            if (!aircraft.hasSeat(seat))
                throw new InvalidRequestException("Seat does not exist on this aircraft: " + seat);
            if (seatToPassenger.put(seat, passenger.name().trim()) != null) {
                throw new InvalidRequestException("Seat requested more than once: " + seat);
            }
        }
        return seatToPassenger;
    }

    /**
     * Returns a booking by its reference.
     *
     * @param pnr booking reference
     * @return the booking (ResourceNotFoundException if unknown)
     */
    @Transactional(readOnly = true)
    public BookingResponse get(String pnr) {
        return BookingResponse.from(bookingRepository
                .findByPnr(pnr)
                .orElseThrow(() -> new ResourceNotFoundException("Booking not found: " + pnr)));
    }

    /**
     * Cancels a booking and releases its seats for rebooking.
     *
     * @param pnr booking reference
     * @return the cancelled booking
     */
    @Transactional
    public BookingResponse cancel(String pnr) {
        // lock the flight first, THEN read the booking so we always see the latest committed state
        Long flightId = bookingRepository
                .findFlightInstanceIdByPnr(pnr)
                .orElseThrow(() -> new ResourceNotFoundException("Booking not found: " + pnr));
        FlightInstance fi = flightInstanceRepository
                .findByIdForUpdate(flightId)
                .orElseThrow(() -> new ResourceNotFoundException("Flight not found: " + flightId));
        Booking b = bookingRepository
                .findByPnr(pnr)
                .orElseThrow(() -> new ResourceNotFoundException("Booking not found: " + pnr));

        if (b.getStatus() == BookingStatus.CANCELLED) throw new ConflictException("Booking is already cancelled");
        Instant now = Instant.now(clock);
        if (!fi.getDepartureUtc().isAfter(now))
            throw new ConflictException("Flight has already departed; cannot cancel");

        b.cancel(now); // status=CANCELLED, seats' active_seat_label=NULL -> seats free again
        bookingRepository.saveAndFlush(b);
        log.info(
                "Booking {} cancelled; released seats {}",
                pnr,
                b.getSeats().stream().map(BookingSeat::getSeatLabel).toList());
        return BookingResponse.from(b);
    }
}
