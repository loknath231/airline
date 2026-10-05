package com.airline.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

import com.airline.config.BookingWindow;
import com.airline.domain.FlightSchedule;
import com.airline.repository.FlightInstanceRepository;
import com.airline.repository.FlightScheduleRepository;
import java.time.Clock;
import java.time.DayOfWeek;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Set;
import org.junit.jupiter.api.Test;
import org.springframework.dao.DataIntegrityViolationException;

class FlightInstanceGeneratorTest {

    @Test
    void mondaysOnly_inFourWeekRange() {
        // 2026-01-05 is a Monday
        List<LocalDate> d = FlightInstanceGenerator.operatingDates(
                Set.of(DayOfWeek.MONDAY), LocalDate.of(2026, 1, 5), LocalDate.of(2026, 1, 26));
        assertEquals(
                List.of(
                        LocalDate.of(2026, 1, 5),
                        LocalDate.of(2026, 1, 12),
                        LocalDate.of(2026, 1, 19),
                        LocalDate.of(2026, 1, 26)),
                d);
    }

    @Test
    void monWedFri_overTwoWeeks_isSix() {
        List<LocalDate> d = FlightInstanceGenerator.operatingDates(
                Set.of(DayOfWeek.MONDAY, DayOfWeek.WEDNESDAY, DayOfWeek.FRIDAY),
                LocalDate.of(2026, 1, 5),
                LocalDate.of(2026, 1, 18));
        assertEquals(6, d.size());
    }

    @Test
    void extendWindow_continuesWithNextSchedule_whenOneFailsConcurrently() {
        FlightInstanceRepository instances = mock(FlightInstanceRepository.class);
        FlightScheduleRepository schedules = mock(FlightScheduleRepository.class);
        BookingWindow window = new BookingWindow(Clock.fixed(Instant.parse("2026-01-05T00:00:00Z"), ZoneOffset.UTC), 6);
        FlightSchedule first = schedule(1L, "XY001");
        FlightSchedule second = schedule(2L, "XY002");
        when(schedules.findAll()).thenReturn(List.of(first, second));
        when(instances.saveAll(any()))
                .thenThrow(new DataIntegrityViolationException("uq_instance_schedule_date"))
                .thenReturn(List.of());

        new FlightInstanceGenerator(instances, schedules, window).extendWindow();

        verify(instances, times(2)).saveAll(any());
    }

    private static FlightSchedule schedule(Long id, String flightNumber) {
        FlightSchedule s = new FlightSchedule();
        s.setId(id);
        s.setFlightNumber(flightNumber);
        s.setDepartureTime(LocalTime.of(9, 0));
        s.setArrivalTime(LocalTime.of(12, 0));
        s.setDaysOfOperation(Set.of(DayOfWeek.MONDAY));
        return s;
    }

    @Test
    void dailySchedule_coversWholeWindowInclusive() {
        LocalDate from = LocalDate.of(2026, 10, 2);
        List<LocalDate> d =
                FlightInstanceGenerator.operatingDates(Set.of(DayOfWeek.values()), from, from.plusDays(365));
        assertEquals(366, d.size());
        assertTrue(d.contains(from.plusDays(365)));
    }
}
