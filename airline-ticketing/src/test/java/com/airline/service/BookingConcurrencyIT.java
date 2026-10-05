package com.airline.service;

import static com.airline.TestFlights.booking;
import static com.airline.TestFlights.newDailyFlight;
import static org.junit.jupiter.api.Assertions.*;

import com.airline.exception.ConflictException;
import com.airline.web.dto.*;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest
class BookingConcurrencyIT {
    @Autowired
    ScheduleService scheduleService;

    @Autowired
    FlightSearchService searchService;

    @Autowired
    SeatMapService seatMapService;

    @Autowired
    BookingService bookingService;

    private int capacity(Long flight) {
        return seatMapService.get(flight).totalSeats();
    }

    private int available(Long flight) {
        return seatMapService.get(flight).availableSeats();
    }

    @Test
    void sameSeat_concurrently_onlyOneWins() throws Exception {
        Long flight = newDailyFlight(scheduleService, searchService);
        int threads = 20;
        ExecutorService pool = Executors.newFixedThreadPool(threads);
        CountDownLatch start = new CountDownLatch(1);
        AtomicInteger ok = new AtomicInteger(), conflict = new AtomicInteger();
        List<Future<?>> fs = new ArrayList<>();
        for (int i = 0; i < threads; i++) {
            final int n = i;
            fs.add(pool.submit(() -> {
                start.await();
                try {
                    bookingService.create(booking(flight, "P" + n, "1A"));
                    ok.incrementAndGet();
                } catch (ConflictException e) {
                    conflict.incrementAndGet();
                }
                return null;
            }));
        }
        start.countDown();
        for (Future<?> f : fs) f.get(60, TimeUnit.SECONDS);
        pool.shutdown();

        assertEquals(1, ok.get());
        assertEquals(threads - 1, conflict.get());
        assertEquals(capacity(flight) - 1, available(flight));
    }

    @Test
    void differentSeats_concurrently_allSucceed() throws Exception {
        Long flight = newDailyFlight(scheduleService, searchService);
        List<String> seats = List.of("1A", "1B", "1C", "2A", "2B", "2C");
        ExecutorService pool = Executors.newFixedThreadPool(seats.size());
        CountDownLatch start = new CountDownLatch(1);
        List<Future<BookingResponse>> fs = new ArrayList<>();
        for (String seat : seats) {
            fs.add(pool.submit(() -> {
                start.await();
                return bookingService.create(booking(flight, "P" + seat, seat));
            }));
        }
        start.countDown();
        for (Future<BookingResponse> f : fs)
            assertEquals("CONFIRMED", f.get(60, TimeUnit.SECONDS).status());
        pool.shutdown();

        assertEquals(capacity(flight) - seats.size(), available(flight));
    }

    @Test
    void cancel_releasesSeats_andTheyCanBeRebooked() {
        Long flight = newDailyFlight(scheduleService, searchService);
        int capacity = capacity(flight);
        BookingResponse b = bookingService.create(booking(flight, "Alice", "2A", "2B"));
        assertEquals("CONFIRMED", b.status());
        assertEquals(capacity - 2, available(flight));
        assertThrows(ConflictException.class, () -> bookingService.create(booking(flight, "Bob", "2B")));

        BookingResponse c = bookingService.cancel(b.pnr());
        assertEquals("CANCELLED", c.status());
        assertNotNull(c.cancelledAt());
        assertEquals(capacity, available(flight));
        assertThrows(ConflictException.class, () -> bookingService.cancel(b.pnr()));

        assertEquals(
                "CONFIRMED", bookingService.create(booking(flight, "Bob", "2B")).status());
    }
}
