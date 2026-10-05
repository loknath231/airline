package com.airline.web.dto;

import com.airline.domain.FlightSchedule;
import java.time.DayOfWeek;
import java.time.LocalTime;
import java.util.List;

public record ScheduleResponse(
        Long id,
        String flightNumber,
        String sourceAirport,
        String destinationAirport,
        LocalTime departureTime,
        LocalTime arrivalTime,
        int arrivalDayOffset,
        Long aircraftId,
        String aircraftType,
        List<DayOfWeek> daysOfOperation,
        Integer instancesGenerated) {
    public static ScheduleResponse from(FlightSchedule s, Integer generated) {
        return new ScheduleResponse(
                s.getId(),
                s.getFlightNumber(),
                s.getSourceAirport().getCode(),
                s.getDestinationAirport().getCode(),
                s.getDepartureTime(),
                s.getArrivalTime(),
                s.getArrivalDayOffset(),
                s.getAircraft().getId(),
                s.getAircraft().getAircraftType(),
                s.getDaysOfOperation().stream().sorted().toList(),
                generated);
    }
}
