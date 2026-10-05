package com.airline.service;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

import com.airline.repository.FlightScheduleRepository;
import com.airline.web.dto.CreateScheduleRequest;
import org.junit.jupiter.api.Test;

/** Demo seeding is disabled in the test suite (see pom.xml), so its behaviour is checked with mocks. */
class DemoDataLoaderTest {
    private final ScheduleService scheduleService = mock(ScheduleService.class);
    private final FlightScheduleRepository schedules = mock(FlightScheduleRepository.class);
    private final DemoDataLoader loader = new DemoDataLoader(scheduleService, schedules);

    @Test
    void seedsDemoSchedules_whenDatabaseHasNone() {
        when(schedules.count()).thenReturn(0L);
        loader.run(null);
        verify(scheduleService, times(32)).create(any(CreateScheduleRequest.class));
    }

    @Test
    void doesNothing_whenSchedulesAlreadyExist() {
        when(schedules.count()).thenReturn(1L);
        loader.run(null);
        verifyNoInteractions(scheduleService);
    }
}
