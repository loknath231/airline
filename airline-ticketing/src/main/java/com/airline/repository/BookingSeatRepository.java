package com.airline.repository;

import com.airline.domain.BookingSeat;
import java.util.Collection;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

/**
 * Supports crud for BookingSeat table
 *
 * @author Loknath Kumar
 * @version 1.0
 * @since 2026-10-04
 */
public interface BookingSeatRepository extends JpaRepository<BookingSeat, Long> {

    @Query("select s.seatLabel from BookingSeat s where s.flightInstance.id = :fid and s.activeSeatLabel is not null")
    List<String> findActiveSeatLabels(@Param("fid") Long flightInstanceId);

    /** Rows of [flightInstanceId, activeSeatCount]. */
    @Query("""
            select s.flightInstance.id, count(s) from BookingSeat s
            where s.flightInstance.id in :ids and s.activeSeatLabel is not null
            group by s.flightInstance.id""")
    List<Object[]> countActiveByInstance(@Param("ids") Collection<Long> ids);
}
