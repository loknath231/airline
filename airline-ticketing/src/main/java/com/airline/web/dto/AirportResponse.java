package com.airline.web.dto;

import com.airline.domain.Airport;

public record AirportResponse(String code, String name, String city, String country) {
    public static AirportResponse from(Airport a) {
        return new AirportResponse(a.getCode(), a.getName(), a.getCity(), a.getCountry());
    }
}
