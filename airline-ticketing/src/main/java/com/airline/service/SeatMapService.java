package com.airline.service;

import com.airline.domain.Aircraft;
import com.airline.domain.FlightInstance;
import com.airline.exception.ResourceNotFoundException;
import com.airline.repository.BookingSeatRepository;
import com.airline.repository.FlightInstanceRepository;
import com.airline.web.dto.SeatMapResponse;
import com.airline.web.dto.SeatMapResponse.SeatStatus;
import com.airline.web.dto.SeatMapResponse.SeatView;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Seat Map service is used for map a seat to flight
 *
 * @author Loknath Kumar
 * @version 1.0
 * @since 2026-10-03
 */
@Service
@RequiredArgsConstructor
public class SeatMapService {
    private final FlightInstanceRepository flightInstanceRepository;
    private final BookingSeatRepository bookingSeatRepository;

    /** Seat map = fixed aircraft layout overlaid with active booked seats (live from DB). */
    @Transactional(readOnly = true)
    public SeatMapResponse get(Long flightInstanceId) {
        FlightInstance fi = flightInstanceRepository
                .findById(flightInstanceId)
                .orElseThrow(() -> new ResourceNotFoundException("Flight not found: " + flightInstanceId));
        Aircraft ac = fi.getSchedule().getAircraft();
        Set<String> booked = new HashSet<>(bookingSeatRepository.findActiveSeatLabels(fi.getId()));
        List<SeatView> seats = ac.seatLabels().stream()
                .map(l -> new SeatView(l, booked.contains(l) ? SeatStatus.BOOKED : SeatStatus.AVAILABLE))
                .toList();
        return new SeatMapResponse(
                fi.getId(),
                fi.getSchedule().getFlightNumber(),
                fi.getFlightDate(),
                ac.getAircraftType(),
                ac.capacity(),
                ac.capacity() - booked.size(),
                seats);
    }
}
