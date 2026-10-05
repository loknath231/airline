package com.airline.config;

import com.airline.exception.InvalidRequestException;
import java.time.Clock;
import java.time.LocalDate;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

/** Rolling booking window: today .. today + N days (UTC). */
@Component
public class BookingWindow {
    private final Clock clock;
    private final int days;

    public BookingWindow(Clock clock, @Value("${app.booking-window-days:365}") int days) {
        this.clock = clock;
        this.days = days;
    }

    public LocalDate today() {
        return LocalDate.now(clock);
    }

    public LocalDate lastBookableDate() {
        return today().plusDays(days);
    }

    public void validate(LocalDate date) {
        if (date.isBefore(today()) || date.isAfter(lastBookableDate())) {
            throw new InvalidRequestException(
                    "Date must be between " + today() + " and " + lastBookableDate() + " (UTC)");
        }
    }
}
