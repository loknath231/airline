package com.airline.domain;

import jakarta.persistence.*;
import java.time.Instant;
import java.time.LocalDate;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Entity for flight schedule details
 * A bookable occurrence of a schedule on one calendar date (materialised).
 *
 * @author Loknath Kumar
 * @version 1.0
 * @since 2026-10-04
 */
@Entity
@Table(name = "flight_instance")
@Getter
@Setter
@NoArgsConstructor
public class FlightInstance {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "schedule_id")
    private FlightSchedule schedule;

    private LocalDate flightDate;
    private Instant departureUtc;
    private Instant arrivalUtc;

    @Enumerated(EnumType.STRING)
    private FlightStatus status = FlightStatus.SCHEDULED;
}
