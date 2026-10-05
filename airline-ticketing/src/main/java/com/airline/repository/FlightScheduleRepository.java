package com.airline.repository;

import com.airline.domain.FlightSchedule;
import org.springframework.data.jpa.repository.JpaRepository;

/**
 * Supports crud for flight_schedule table
 *
 * @author Loknath Kumar
 * @version 1.0
 * @since 2026-10-04
 */
public interface FlightScheduleRepository extends JpaRepository<FlightSchedule, Long> {
    boolean existsByFlightNumber(String flightNumber);
}
