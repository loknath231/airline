package com.airline.web.dto;

import java.time.LocalDate;
import java.util.List;

public record SeatMapResponse(
        Long flightInstanceId,
        String flightNumber,
        LocalDate flightDate,
        String aircraftType,
        int totalSeats,
        int availableSeats,
        List<SeatView> seats) {
    public enum SeatStatus {
        AVAILABLE,
        BOOKED
    }

    public record SeatView(String seat, SeatStatus status) {}
}
