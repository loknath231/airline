package com.airline.repository;

import com.airline.domain.Airport;
import org.springframework.data.jpa.repository.JpaRepository;

/**
 * Supports crud for Airport table
 *
 * @author Loknath Kumar
 * @version 1.0
 * @since 2026-10-04
 */
public interface AirportRepository extends JpaRepository<Airport, String> {}
