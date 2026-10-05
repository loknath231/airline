package com.airline.service;

import com.airline.domain.Aircraft;
import com.airline.domain.Airport;
import com.airline.domain.FlightSchedule;
import com.airline.exception.ConflictException;
import com.airline.exception.InvalidRequestException;
import com.airline.exception.ResourceNotFoundException;
import com.airline.repository.AircraftRepository;
import com.airline.repository.AirportRepository;
import com.airline.repository.FlightScheduleRepository;
import com.airline.web.dto.CreateScheduleRequest;
import com.airline.web.dto.ScheduleResponse;
import java.time.Clock;
import java.time.Instant;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Schedule service is used for finding a flight schedule
 *
 * @author Loknath Kumar
 * @version 1.0
 * @since 2026-10-03
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class ScheduleService {
    private final FlightScheduleRepository flightScheduleRepository;
    private final AirportRepository airportRepository;
    private final AircraftRepository aircraftRepository;
    private final FlightInstanceGenerator flightInstanceGenerator;
    private final Clock clock;

    /**
     * To search flight for booking
     *
     * @param createScheduleRequest details for creating a schedule of a flight
     *
     * @return ScheduleResponse
     */
    @Transactional
    public ScheduleResponse create(CreateScheduleRequest createScheduleRequest) {
        String flightNumber = createScheduleRequest.flightNumber().trim().toUpperCase();
        if (flightScheduleRepository.existsByFlightNumber(flightNumber)) {
            throw new ConflictException("Flight number already exists: " + flightNumber);
        }
        Airport src = airport(createScheduleRequest.sourceAirport());
        Airport dst = airport(createScheduleRequest.destinationAirport());
        if (src.getCode().equals(dst.getCode())) {
            throw new InvalidRequestException("Source and destination airports must differ");
        }
        Aircraft ac = aircraftRepository
                .findById(createScheduleRequest.aircraftId())
                .orElseThrow(() ->
                        new ResourceNotFoundException("Aircraft not found: " + createScheduleRequest.aircraftId()));
        int offset = createScheduleRequest.arrivalDayOffset() == null ? 0 : createScheduleRequest.arrivalDayOffset();
        int depMin = createScheduleRequest.departureTime().getHour() * 60
                + createScheduleRequest.departureTime().getMinute();
        int arrMin = offset * 1440
                + createScheduleRequest.arrivalTime().getHour() * 60
                + createScheduleRequest.arrivalTime().getMinute();
        if (arrMin <= depMin) {
            throw new InvalidRequestException(
                    "Arrival must be after departure (use arrivalDayOffset for next-day arrivals)");
        }

        FlightSchedule flightSchedule = new FlightSchedule();
        flightSchedule.setFlightNumber(flightNumber);
        flightSchedule.setSourceAirport(src);
        flightSchedule.setDestinationAirport(dst);
        flightSchedule.setDepartureTime(createScheduleRequest.departureTime());
        flightSchedule.setArrivalTime(createScheduleRequest.arrivalTime());
        flightSchedule.setArrivalDayOffset(offset);
        flightSchedule.setAircraft(ac);
        flightSchedule.setDaysOfOperation(createScheduleRequest.daysOfOperation());
        flightSchedule.setCreatedAt(Instant.now(clock));
        flightScheduleRepository.save(flightSchedule);

        int generated = flightInstanceGenerator.generate(flightSchedule);
        log.info(
                "Created schedule {} {}->{} days={}",
                flightNumber,
                src.getCode(),
                dst.getCode(),
                createScheduleRequest.daysOfOperation());
        return ScheduleResponse.from(flightSchedule, generated);
    }

    @Transactional(readOnly = true)
    public ScheduleResponse get(Long id) {
        return flightScheduleRepository
                .findById(id)
                .map(s -> ScheduleResponse.from(s, null))
                .orElseThrow(() -> new ResourceNotFoundException("Schedule not found: " + id));
    }

    @Transactional(readOnly = true)
    public List<ScheduleResponse> list() {
        return flightScheduleRepository.findAll().stream()
                .map(s -> ScheduleResponse.from(s, null))
                .toList();
    }

    private Airport airport(String code) {
        String c = code.trim().toUpperCase();
        return airportRepository
                .findById(c)
                .orElseThrow(() -> new ResourceNotFoundException("Airport not found: " + c));
    }
}
