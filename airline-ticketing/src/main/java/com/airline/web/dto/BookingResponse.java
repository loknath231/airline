package com.airline.web.dto;

import com.airline.domain.Booking;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;

public record BookingResponse(
        String pnr,
        Long flightInstanceId,
        String flightNumber,
        LocalDate flightDate,
        Instant departureTime,
        int passengerCount,
        List<String> seats,
        List<PassengerView> passengers,
        String status,
        Instant createdAt,
        Instant cancelledAt) {

    public record PassengerView(String name, String seat) {}

    public static BookingResponse from(Booking b) {
        var fi = b.getFlightInstance();
        var passengers = b.getSeats().stream()
                .map(s -> new PassengerView(s.getPassengerName(), s.getSeatLabel()))
                .toList();
        return new BookingResponse(
                b.getPnr(),
                fi.getId(),
                fi.getSchedule().getFlightNumber(),
                fi.getFlightDate(),
                fi.getDepartureUtc(),
                b.getPassengerCount(),
                passengers.stream().map(PassengerView::seat).toList(),
                passengers,
                b.getStatus().name(),
                b.getCreatedAt(),
                b.getCancelledAt());
    }
}
