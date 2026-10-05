package com.airline.web.dto;

import java.time.Instant;
import java.time.LocalDate;

public record FlightSearchResult(
        Long flightInstanceId,
        String flightNumber,
        String sourceAirport,
        String destinationAirport,
        LocalDate flightDate,
        Instant departureTime,
        Instant arrivalTime,
        int availableSeats,
        int totalSeats) {}
