package com.airline.service;

import com.airline.config.BookingWindow;
import com.airline.domain.FlightInstance;
import com.airline.domain.FlightSchedule;
import com.airline.domain.FlightStatus;
import com.airline.repository.FlightInstanceRepository;
import com.airline.repository.FlightScheduleRepository;
import java.time.DayOfWeek;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Hybrid flight-instance strategy: instances are materialised for the rolling window
 * [today, today + 365d] when a schedule is created, and the window is extended nightly.
 * Generation is idempotent (existing dates skipped; UNIQUE(schedule_id, flight_date) backs it up).
 *
 * @author Loknath Kumar
 * @version 1.0
 * @since 2026-10-04
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class FlightInstanceGenerator {
    private final FlightInstanceRepository flightInstanceRepository;
    private final FlightScheduleRepository scheduleRepository;
    private final BookingWindow window;

    /**
     * Pure date matching: every date in [from, to] (inclusive) whose weekday is operated.
     *
     * @param days days of the week the schedule operates
     * @param from first date of the range
     * @param to last date of the range
     * @return matching dates in ascending order
     */
    public static List<LocalDate> operatingDates(Set<DayOfWeek> days, LocalDate from, LocalDate to) {
        return from.datesUntil(to.plusDays(1))
                .filter(d -> days.contains(d.getDayOfWeek()))
                .toList();
    }

    /**
     * Materialises the missing flight instances of one schedule for the booking window. Idempotent.
     *
     * @param flightSchedule a persisted schedule
     * @return number of instances created
     */
    @Transactional
    public int generate(FlightSchedule flightSchedule) {
        LocalDate from = window.today();
        LocalDate to = window.lastBookableDate();
        Set<LocalDate> existing =
                new HashSet<>(flightInstanceRepository.findExistingDates(flightSchedule.getId(), from, to));
        List<FlightInstance> toCreate = operatingDates(flightSchedule.getDaysOfOperation(), from, to).stream()
                .filter(date -> !existing.contains(date))
                .map(date -> toInstance(flightSchedule, date))
                .toList();
        flightInstanceRepository.saveAll(toCreate);
        log.info(
                "Generated {} flight instances for {} ({}..{})",
                toCreate.size(),
                flightSchedule.getFlightNumber(),
                from,
                to);
        return toCreate.size();
    }

    /**
     * Nightly roll-forward so every schedule always covers a full year. Each schedule is committed on its own
     * (no surrounding transaction), so one failure, e.g. another app instance generating the same dates at the
     * same time, does not roll back the others.
     */
    @Scheduled(cron = "0 5 0 * * *", zone = "UTC")
    public void extendWindow() {
        for (FlightSchedule schedule : scheduleRepository.findAll()) {
            try {
                generate(schedule);
            } catch (DataIntegrityViolationException e) {
                log.warn("Skipped {}: instances were generated concurrently", schedule.getFlightNumber());
            }
        }
    }

    /**
     * Builds the instance of a schedule for one date, with UTC departure and arrival timestamps.
     *
     * @param flightSchedule schedule (template)
     * @param date flight date (UTC)
     * @return a new, unsaved flight instance
     */
    private FlightInstance toInstance(FlightSchedule flightSchedule, LocalDate date) {
        FlightInstance flightInstance = new FlightInstance();
        flightInstance.setSchedule(flightSchedule);
        flightInstance.setFlightDate(date);
        Instant dep = LocalDateTime.of(date, flightSchedule.getDepartureTime()).toInstant(ZoneOffset.UTC);
        Instant arr = LocalDateTime.of(
                        date.plusDays(flightSchedule.getArrivalDayOffset()), flightSchedule.getArrivalTime())
                .toInstant(ZoneOffset.UTC);
        flightInstance.setDepartureUtc(dep);
        flightInstance.setArrivalUtc(arr);
        flightInstance.setStatus(FlightStatus.SCHEDULED);
        return flightInstance;
    }
}
