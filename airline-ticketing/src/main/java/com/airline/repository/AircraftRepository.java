package com.airline.repository;

import com.airline.domain.Aircraft;
import org.springframework.data.jpa.repository.JpaRepository;

/**
 * Supports crud for Aircraft table
 *
 * @author Loknath Kumar
 * @version 1.0
 * @since 2026-10-04
 */
public interface AircraftRepository extends JpaRepository<Aircraft, Long> {}
