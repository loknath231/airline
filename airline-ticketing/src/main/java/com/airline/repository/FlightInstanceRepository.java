package com.airline.repository;

import com.airline.domain.FlightInstance;
import com.airline.domain.FlightStatus;
import jakarta.persistence.LockModeType;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

/**
 * Supports crud for flight_instance table
 *
 * @author Loknath Kumar
 * @version 1.0
 * @since 2026-10-04
 */
public interface FlightInstanceRepository extends JpaRepository<FlightInstance, Long> {

    /** SELECT ... FOR UPDATE on the flight instance row: serialises seat changes per flight. */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select f from FlightInstance f where f.id = :id")
    Optional<FlightInstance> findByIdForUpdate(@Param("id") Long id);

    @Query("""
            select f from FlightInstance f join fetch f.schedule s join fetch s.aircraft
            where s.sourceAirport.code = :src and s.destinationAirport.code = :dst
              and f.flightDate = :date and f.status = :status
            order by f.departureUtc""")
    List<FlightInstance> search(
            @Param("src") String src,
            @Param("dst") String dst,
            @Param("date") LocalDate date,
            @Param("status") FlightStatus status);

    @Query(
            "select f.flightDate from FlightInstance f where f.schedule.id = :sid and f.flightDate between :from and :to")
    List<LocalDate> findExistingDates(
            @Param("sid") Long scheduleId, @Param("from") LocalDate from, @Param("to") LocalDate to);
}
