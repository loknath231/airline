package com.airline.config;

import static org.junit.jupiter.api.Assertions.*;

import com.airline.exception.InvalidRequestException;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import org.junit.jupiter.api.Test;

class BookingWindowTest {
    private final BookingWindow window =
            new BookingWindow(Clock.fixed(Instant.parse("2026-10-04T10:00:00Z"), ZoneOffset.UTC), 365);

    @Test
    void windowIsTodayPlusConfiguredDays() {
        assertEquals(LocalDate.of(2026, 10, 4), window.today());
        assertEquals(LocalDate.of(2027, 10, 4), window.lastBookableDate());
    }

    @Test
    void boundariesAreInclusive() {
        assertDoesNotThrow(() -> window.validate(LocalDate.of(2026, 10, 4)));
        assertDoesNotThrow(() -> window.validate(LocalDate.of(2027, 10, 4)));
    }

    @Test
    void datesOutsideWindowAreRejected() {
        InvalidRequestException past =
                assertThrows(InvalidRequestException.class, () -> window.validate(LocalDate.of(2026, 10, 3)));
        assertTrue(past.getMessage().contains("2026-10-04"));
        assertThrows(InvalidRequestException.class, () -> window.validate(LocalDate.of(2027, 10, 5)));
    }
}
