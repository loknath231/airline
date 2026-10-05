package com.airline.web;

import com.airline.service.FlightSearchService;
import com.airline.service.SeatMapService;
import com.airline.web.dto.FlightSearchResult;
import com.airline.web.dto.SeatMapResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.time.LocalDate;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.*;

/**
 * Controller to do Search and Seat Mapping
 *
 * @author Loknath Kumar
 * @version 1.0
 * @since 2026-03-01
 */
@RestController
@RequestMapping("/api/v1/flights")
@RequiredArgsConstructor
@Tag(name = "Flights", description = "Search and seat maps")
public class FlightController {
    private final FlightSearchService searchService;
    private final SeatMapService seatMapService;

    /**
     * Finds the flights operating on a route on one date, with live seat availability.
     *
     * @param source source airport IATA code
     * @param destination destination airport IATA code
     * @param date travel date (UTC, yyyy-MM-dd)
     * @return matching flights ordered by departure; flights that have already departed are excluded
     */
    @GetMapping("/search")
    @Operation(summary = "Search flights by source, destination and date (UTC, yyyy-MM-dd)")
    public List<FlightSearchResult> search(
            @RequestParam String source,
            @RequestParam String destination,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date) {
        return searchService.search(source, destination, date);
    }

    /**
     * Returns the real-time seat map of one flight instance.
     *
     * @param flightInstanceId flight instance id (from search results)
     * @return every seat of the aircraft marked AVAILABLE or BOOKED
     */
    @GetMapping("/{flightInstanceId}/seats")
    @Operation(summary = "Real-time seat map of a flight instance")
    public SeatMapResponse seats(@PathVariable Long flightInstanceId) {
        return seatMapService.get(flightInstanceId);
    }
}
