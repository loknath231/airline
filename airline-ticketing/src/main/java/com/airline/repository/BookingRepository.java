package com.airline.repository;

import com.airline.domain.Booking;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

/**
 * Supports crud for Booking table
 *
 * @author Loknath Kumar
 * @version 1.0
 * @since 2026-10-04
 */
public interface BookingRepository extends JpaRepository<Booking, Long> {
    Optional<Booking> findByPnr(String pnr);

    @Query("select b.flightInstance.id from Booking b where b.pnr = :pnr")
    Optional<Long> findFlightInstanceIdByPnr(@Param("pnr") String pnr);
}
