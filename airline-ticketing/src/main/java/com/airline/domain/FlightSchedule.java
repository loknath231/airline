package com.airline.domain;

import jakarta.persistence.*;
import java.time.DayOfWeek;
import java.time.Instant;
import java.time.LocalTime;
import java.util.HashSet;
import java.util.Set;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Entity for flight schedule details
 *
 * @author Loknath Kumar
 * @version 1.0
 * @since 2026-10-04
 */
@Entity
@Table(name = "flight_schedule")
@Getter
@Setter
@NoArgsConstructor
public class FlightSchedule {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String flightNumber;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "source_airport")
    private Airport sourceAirport;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "destination_airport")
    private Airport destinationAirport;

    private LocalTime departureTime;
    private LocalTime arrivalTime;
    private int arrivalDayOffset;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "aircraft_id")
    private Aircraft aircraft;

    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(name = "schedule_operating_day", joinColumns = @JoinColumn(name = "schedule_id"))
    @Column(name = "day_of_week")
    @Enumerated(EnumType.STRING)
    private Set<DayOfWeek> daysOfOperation = new HashSet<>();

    private Instant createdAt;
}
