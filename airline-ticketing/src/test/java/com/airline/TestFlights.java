package com.airline;

import com.airline.service.FlightSearchService;
import com.airline.service.ScheduleService;
import com.airline.web.dto.CreateBookingRequest;
import com.airline.web.dto.CreateScheduleRequest;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.ZoneOffset;
import java.util.Arrays;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;

/** Shared helpers for integration tests (all test classes share one in-memory H2 database). */
public final class TestFlights {
    private static final AtomicInteger SEQ = new AtomicInteger();

    private TestFlights() {}

    /** Flight number unique within the JVM, matching ^[A-Za-z0-9]{3,8}$. */
    public static String uniqueFlightNumber(String prefix) {
        return prefix + String.format("%05d", SEQ.incrementAndGet());
    }

    /** Creates a daily DXB->LHR schedule on aircraft 1 and returns the instance id two days from now. */
    public static Long newDailyFlight(ScheduleService schedules, FlightSearchService search) {
        String fn = uniqueFlightNumber("T");
        schedules.create(new CreateScheduleRequest(
                fn, "DXB", "LHR", LocalTime.of(9, 30), LocalTime.of(13, 45), 0, 1L, Set.of(DayOfWeek.values())));
        LocalDate date = LocalDate.now(ZoneOffset.UTC).plusDays(2);
        return search.search("DXB", "LHR", date).stream()
                .filter(f -> f.flightNumber().equals(fn))
                .findFirst()
                .orElseThrow()
                .flightInstanceId();
    }

    public static CreateBookingRequest booking(Long flight, String name, String... seats) {
        return new CreateBookingRequest(
                flight,
                Arrays.stream(seats)
                        .map(s -> new CreateBookingRequest.PassengerRequest(name, s))
                        .toList());
    }
}
