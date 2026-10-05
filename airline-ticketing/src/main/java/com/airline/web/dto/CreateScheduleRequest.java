package com.airline.web.dto;

import jakarta.validation.constraints.*;
import java.time.DayOfWeek;
import java.time.LocalTime;
import java.util.Set;

public record CreateScheduleRequest(
        @NotBlank @Pattern(regexp = "^[A-Za-z0-9]{3,8}$", message = "must be 3-8 alphanumeric characters")
        String flightNumber,

        @NotBlank @Size(min = 3, max = 3, message = "must be a 3-letter IATA code")
        String sourceAirport,

        @NotBlank @Size(min = 3, max = 3, message = "must be a 3-letter IATA code")
        String destinationAirport,

        @NotNull LocalTime departureTime,
        @NotNull LocalTime arrivalTime,
        @Min(0) @Max(3) Integer arrivalDayOffset,
        @NotNull Long aircraftId,
        @NotEmpty Set<DayOfWeek> daysOfOperation) {}
