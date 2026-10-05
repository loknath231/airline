package com.airline.service;

import com.airline.config.BookingWindow;
import com.airline.domain.FlightInstance;
import com.airline.domain.FlightStatus;
import com.airline.exception.InvalidRequestException;
import com.airline.exception.ResourceNotFoundException;
import com.airline.repository.AirportRepository;
import com.airline.repository.BookingSeatRepository;
import com.airline.repository.FlightInstanceRepository;
import com.airline.web.dto.FlightSearchResult;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Flight search service is used for finding a flight
 *
 * @author Loknath Kumar
 * @version 1.0
 * @since 2026-10-03
 */
@Service
@RequiredArgsConstructor
public class FlightSearchService {
    private final FlightInstanceRepository flightInstanceRepository;
    private final BookingSeatRepository bookingSeatRepository;
    private final AirportRepository airportRepository;
    private final BookingWindow bookingWindow;
    private final Clock clock;

    /**
     * To search flight for booking
     *
     * @param source source
     * @param destination destination
     * @param date localDate in UTC
     * @return list of FlightSearchedResult
     */
    @Transactional(readOnly = true)
    public List<FlightSearchResult> search(String source, String destination, LocalDate date) {
        if (source == null || source.isBlank() || destination == null || destination.isBlank() || date == null) {
            throw new InvalidRequestException("source, destination and date are required");
        }
        String src = source.trim().toUpperCase();
        String dst = destination.trim().toUpperCase();
        if (src.equals(dst)) throw new InvalidRequestException("Source and destination must differ");
        bookingWindow.validate(date);
        for (String code : List.of(src, dst)) {
            if (!airportRepository.existsById(code)) throw new ResourceNotFoundException("Airport not found: " + code);
        }

        Instant now = Instant.now(clock);
        List<FlightInstance> found = flightInstanceRepository.search(src, dst, date, FlightStatus.SCHEDULED).stream()
                .filter(flightInstance -> flightInstance.getDepartureUtc().isAfter(now))
                .toList();
        if (found.isEmpty()) return List.of();

        Map<Long, Long> booked = new HashMap<>();
        for (Object[] row : bookingSeatRepository.countActiveByInstance(
                found.stream().map(FlightInstance::getId).toList())) {
            booked.put(((Number) row[0]).longValue(), ((Number) row[1]).longValue());
        }
        return found.stream()
                .map(flightInstance -> {
                    int total = flightInstance.getSchedule().getAircraft().capacity();
                    int available = total
                            - booked.getOrDefault(flightInstance.getId(), 0L).intValue();
                    return new FlightSearchResult(
                            flightInstance.getId(),
                            flightInstance.getSchedule().getFlightNumber(),
                            src,
                            dst,
                            flightInstance.getFlightDate(),
                            flightInstance.getDepartureUtc(),
                            flightInstance.getArrivalUtc(),
                            available,
                            total);
                })
                .toList();
    }
}
