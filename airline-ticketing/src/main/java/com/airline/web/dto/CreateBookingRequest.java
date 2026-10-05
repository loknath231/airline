package com.airline.web.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.util.List;

public record CreateBookingRequest(
        @NotNull Long flightInstanceId,

        @NotEmpty @Size(max = 9, message = "at most 9 passengers per booking")
        List<@Valid PassengerRequest> passengers) {

    public record PassengerRequest(
            @NotBlank @Size(max = 100) String name,
            @NotBlank @Size(max = 4) String seat) {}
}
