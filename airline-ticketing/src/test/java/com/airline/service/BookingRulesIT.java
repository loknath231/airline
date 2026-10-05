package com.airline.service;

import static com.airline.TestFlights.booking;
import static com.airline.TestFlights.newDailyFlight;
import static org.junit.jupiter.api.Assertions.*;

import com.airline.domain.FlightInstance;
import com.airline.domain.FlightStatus;
import com.airline.exception.ConflictException;
import com.airline.exception.InvalidRequestException;
import com.airline.exception.ResourceNotFoundException;
import com.airline.repository.FlightInstanceRepository;
import com.airline.web.dto.BookingResponse;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.function.Consumer;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

/** Business-rule branches of BookingService, FlightSearchService and FlightInstanceGenerator. */
@SpringBootTest
class BookingRulesIT {
    @Autowired
    ScheduleService scheduleService;

    @Autowired
    FlightSearchService searchService;

    @Autowired
    BookingService bookingService;

    @Autowired
    FlightInstanceGenerator generator;

    @Autowired
    FlightInstanceRepository instances;

    private void update(Long flight, Consumer<FlightInstance> change) {
        var fi = instances.findById(flight).orElseThrow();
        change.accept(fi);
        instances.save(fi);
    }

    @Test
    void booking_returnsPassengersAndNormalisesSeats() {
        Long flight = newDailyFlight(scheduleService, searchService);
        BookingResponse b = bookingService.create(booking(flight, " Ann ", "3a", " 3B "));

        assertEquals(List.of("3A", "3B"), b.seats());
        assertEquals("Ann", b.passengers().get(0).name());
        assertEquals(2, b.passengerCount());
        assertEquals(b.pnr(), bookingService.get(b.pnr()).pnr());
    }

    @Test
    void seatNotOnAircraft_isRejected() {
        Long flight = newDailyFlight(scheduleService, searchService);
        var e = assertThrows(InvalidRequestException.class, () -> bookingService.create(booking(flight, "Ann", "99Z")));
        assertEquals("Seat does not exist on this aircraft: 99Z", e.getMessage());
    }

    @Test
    void sameSeatTwiceInOneRequest_isRejected() {
        Long flight = newDailyFlight(scheduleService, searchService);
        var e = assertThrows(
                InvalidRequestException.class, () -> bookingService.create(booking(flight, "Ann", "4A", "4a")));
        assertEquals("Seat requested more than once: 4A", e.getMessage());
    }

    @Test
    void cancelledFlight_isNotBookable() {
        Long flight = newDailyFlight(scheduleService, searchService);
        update(flight, fi -> fi.setStatus(FlightStatus.CANCELLED));
        var e = assertThrows(ConflictException.class, () -> bookingService.create(booking(flight, "Ann", "1A")));
        assertEquals("Flight is not open for booking", e.getMessage());
    }

    @Test
    void departedFlight_cannotBeBooked_orCancelled_andIsHiddenFromSearch() {
        Long flight = newDailyFlight(scheduleService, searchService);
        BookingResponse b = bookingService.create(booking(flight, "Ann", "5A"));
        var date = b.flightDate();

        update(flight, fi -> fi.setDepartureUtc(Instant.now().minus(1, ChronoUnit.HOURS)));

        var booked = assertThrows(ConflictException.class, () -> bookingService.create(booking(flight, "Ben", "5B")));
        assertEquals("Flight has already departed", booked.getMessage());
        var e = assertThrows(ConflictException.class, () -> bookingService.cancel(b.pnr()));
        assertEquals("Flight has already departed; cannot cancel", e.getMessage());
        assertTrue(searchService.search("DXB", "LHR", date).stream()
                .noneMatch(r -> r.flightInstanceId().equals(flight)));
    }

    @Test
    void unknownPnr_isNotFound() {
        assertThrows(ResourceNotFoundException.class, () -> bookingService.get("missing"));
        assertThrows(ResourceNotFoundException.class, () -> bookingService.cancel("missing"));
    }

    @Test
    void search_reportsAvailabilityPerFlight() {
        Long flight = newDailyFlight(scheduleService, searchService);
        bookingService.create(booking(flight, "Ann", "6A", "6B", "6C"));
        var date = bookingService.create(booking(flight, "Ben", "7A")).flightDate();

        var result = searchService.search("dxb", "lhr", date).stream()
                .filter(r -> r.flightInstanceId().equals(flight))
                .findFirst()
                .orElseThrow();
        assertEquals(result.totalSeats() - 4, result.availableSeats());
        assertEquals("DXB", result.sourceAirport());
    }

    @Test
    void extendWindow_isIdempotent() {
        newDailyFlight(scheduleService, searchService);
        long before = instances.count();
        generator.extendWindow();
        assertEquals(before, instances.count());
    }
}
