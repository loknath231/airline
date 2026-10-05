package com.airline.web;

import com.airline.service.ScheduleService;
import com.airline.web.dto.CreateScheduleRequest;
import com.airline.web.dto.ScheduleResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.net.URI;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

/**
 * Back-office API for flight schedules (authentication is out of scope).
 *
 * @author Loknath Kumar
 * @version 1.0
 * @since 2026-10-04
 */
@RestController
@RequestMapping("/api/v1/admin/schedules")
@RequiredArgsConstructor
@Tag(name = "Back Office", description = "Flight schedule management (authentication out of scope)")
public class    AdminScheduleController {
    private final ScheduleService scheduleService;

    /**
     * Creates a flight schedule and materialises its flight instances for the booking window.
     *
     * @param createScheduleRequest flight number, route, times, aircraft and days of operation
     * @return 201 with the created schedule and a {@code Location} header pointing to it
     */
    @PostMapping
    @Operation(summary = "Create a flight schedule and generate 365 days of flight instances")
    public ResponseEntity<ScheduleResponse> createFlightSchedule(
            @Valid @RequestBody CreateScheduleRequest createScheduleRequest) {
        ScheduleResponse created = scheduleService.create(createScheduleRequest);
        return ResponseEntity.created(URI.create("/api/v1/admin/schedules/" + created.id()))
                .body(created);
    }

    /**
     * Returns one schedule.
     *
     * @param id schedule id
     * @return the schedule (404 if unknown)
     */
    @GetMapping("/{id}")
    @Operation(summary = "Get a schedule by id")
    public ScheduleResponse get(@PathVariable Long id) {
        return scheduleService.get(id);
    }

    /**
     * Lists all schedules.
     *
     * @return every schedule, without instance counts
     */
    @GetMapping
    @Operation(summary = "List all schedules")
    public List<ScheduleResponse> list() {
        return scheduleService.list();
    }
}
