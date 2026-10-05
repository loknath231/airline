package com.airline.service;

import com.airline.repository.FlightScheduleRepository;
import com.airline.web.dto.CreateScheduleRequest;
import java.time.DayOfWeek;
import java.time.LocalTime;
import java.util.Set;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

/**
 * Seeds sample schedules on start so the API can be tried immediately.
 * set app.seed-demo-data=true
 *
 * @author Loknath Kumar
 * @version 1.0
 * @since 2026-10-04
 */
@Component
@RequiredArgsConstructor
@Slf4j
@ConditionalOnProperty(name = "app.seed-demo-data", havingValue = "true")
public class DemoDataLoader implements ApplicationRunner {
    private final ScheduleService scheduleService;
    private final FlightScheduleRepository schedules;

    @Override
    public void run(ApplicationArguments args) {
        if (schedules.count() > 0) return;
        // Pre-defined DayOfWeek Sets for convenience
        Set<DayOfWeek> mwf = Set.of(DayOfWeek.MONDAY, DayOfWeek.WEDNESDAY, DayOfWeek.FRIDAY);
        Set<DayOfWeek> tth = Set.of(DayOfWeek.TUESDAY, DayOfWeek.THURSDAY);
        Set<DayOfWeek> weekend = Set.of(DayOfWeek.SATURDAY, DayOfWeek.SUNDAY);
        Set<DayOfWeek> daily = Set.of(DayOfWeek.values());

        // Middle East <-> Europe Routes
        scheduleService.create(new CreateScheduleRequest(
                "XY101", "DXB", "LHR", LocalTime.of(9, 30), LocalTime.of(13, 45), 0, 15L, mwf));
        scheduleService.create(new CreateScheduleRequest(
                "XY102", "LHR", "DXB", LocalTime.of(15, 30), LocalTime.of(1, 15), 1, 15L, mwf));
        scheduleService.create(new CreateScheduleRequest(
                "XY103", "DXB", "CDG", LocalTime.of(8, 0), LocalTime.of(12, 55), 0, 13L, daily));
        scheduleService.create(new CreateScheduleRequest(
                "XY104", "CDG", "DXB", LocalTime.of(14, 30), LocalTime.of(23, 15), 0, 13L, daily));
        scheduleService.create(new CreateScheduleRequest(
                "XY105", "AUH", "FRA", LocalTime.of(2, 15), LocalTime.of(6, 50), 0, 33L, tth));
        scheduleService.create(new CreateScheduleRequest(
                "XY106", "FRA", "AUH", LocalTime.of(11, 20), LocalTime.of(19, 45), 0, 33L, tth));

        // India Domestic & Regional Routes
        scheduleService.create(new CreateScheduleRequest(
                "XY301", "DXB", "DEL", LocalTime.of(9, 30), LocalTime.of(13, 45), 0, 3L, mwf));
        scheduleService.create(new CreateScheduleRequest(
                "XY302", "DEL", "DXB", LocalTime.of(15, 30), LocalTime.of(1, 15), 1, 3L, mwf));
        scheduleService.create(new CreateScheduleRequest(
                "XY303", "DEL", "BOM", LocalTime.of(2, 0), LocalTime.of(3, 45), 0, 4L, daily));
        scheduleService.create(new CreateScheduleRequest(
                "XY304", "BOM", "DEL", LocalTime.of(6, 0), LocalTime.of(7, 50), 0, 4L, daily));
        scheduleService.create(new CreateScheduleRequest(
                "XY305", "BOM", "BLR", LocalTime.of(18, 15), LocalTime.of(19, 55), 0, 5L, daily));
        scheduleService.create(new CreateScheduleRequest(
                "XY306", "BLR", "BOM", LocalTime.of(21, 0), LocalTime.of(22, 45), 0, 5L, daily));
        scheduleService.create(new CreateScheduleRequest(
                "XY307", "MAA", "CCU", LocalTime.of(10, 30), LocalTime.of(12, 45), 0, 6L, mwf));
        scheduleService.create(new CreateScheduleRequest(
                "XY308", "CCU", "MAA", LocalTime.of(13, 30), LocalTime.of(15, 55), 0, 6L, mwf));

        // Transatlantic Routes
        scheduleService.create(new CreateScheduleRequest(
                "XY201", "LHR", "JFK", LocalTime.of(8, 30), LocalTime.of(11, 30), 0, 30L, daily));
        scheduleService.create(new CreateScheduleRequest(
                "XY202", "JFK", "LHR", LocalTime.of(20, 0), LocalTime.of(8, 10), 1, 30L, daily));
        scheduleService.create(new CreateScheduleRequest(
                "XY203", "CDG", "LAX", LocalTime.of(13, 15), LocalTime.of(16, 20), 0, 14L, daily));
        scheduleService.create(new CreateScheduleRequest(
                "XY204", "LAX", "CDG", LocalTime.of(19, 30), LocalTime.of(15, 15), 1, 14L, daily));
        scheduleService.create(new CreateScheduleRequest(
                "XY205", "FRA", "ORD", LocalTime.of(10, 45), LocalTime.of(13, 0), 0, 32L, weekend));
        scheduleService.create(new CreateScheduleRequest(
                "XY206", "ORD", "FRA", LocalTime.of(16, 15), LocalTime.of(7, 40), 1, 32L, weekend));

        // Transpacific & East Asia Routes
        scheduleService.create(new CreateScheduleRequest(
                "XY401", "LAX", "HND", LocalTime.of(12, 45), LocalTime.of(16, 30), 1, 34L, daily));
        scheduleService.create(new CreateScheduleRequest(
                "XY402", "HND", "LAX", LocalTime.of(18, 20), LocalTime.of(12, 10), 1, 34L, daily));
        scheduleService.create(new CreateScheduleRequest(
                "XY403", "SFO", "SIN", LocalTime.of(23, 30), LocalTime.of(6, 40), 2, 33L, daily));
        scheduleService.create(new CreateScheduleRequest(
                "XY404", "SIN", "SFO", LocalTime.of(9, 20), LocalTime.of(8, 55), 1, 33L, daily));
        scheduleService.create(new CreateScheduleRequest(
                "XY405", "HKG", "SYD", LocalTime.of(23, 55), LocalTime.of(11, 10), 1, 9L, tth));
        scheduleService.create(new CreateScheduleRequest(
                "XY406", "SYD", "HKG", LocalTime.of(13, 45), LocalTime.of(21, 0), 0, 9L, tth));

        // North American Domestic Routes
        scheduleService.create(new CreateScheduleRequest(
                "XY501", "JFK", "LAX", LocalTime.of(7, 0), LocalTime.of(10, 15), 0, 18L, daily));
        scheduleService.create(new CreateScheduleRequest(
                "XY502", "LAX", "JFK", LocalTime.of(12, 0), LocalTime.of(20, 30), 0, 18L, daily));
        scheduleService.create(new CreateScheduleRequest(
                "XY503", "ORD", "DFW", LocalTime.of(9, 0), LocalTime.of(11, 25), 0, 20L, daily));
        scheduleService.create(new CreateScheduleRequest(
                "XY504", "DFW", "ORD", LocalTime.of(13, 0), LocalTime.of(15, 20), 0, 20L, daily));
        scheduleService.create(new CreateScheduleRequest(
                "XY505", "YYZ", "YVR", LocalTime.of(17, 0), LocalTime.of(19, 15), 0, 4L, daily));
        scheduleService.create(new CreateScheduleRequest(
                "XY506", "YVR", "YYZ", LocalTime.of(8, 30), LocalTime.of(16, 0), 0, 4L, daily));

        log.info("Demo schedules created");
    }
}
