package com.airline.domain;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Entity for booking seat
 *
 * @author Loknath Kumar
 * @version 1.0
 * @since 2026-10-04
 */
@Entity
@Table(name = "booking_seat")
@Getter
@Setter
@NoArgsConstructor
public class BookingSeat {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "booking_id")
    private Booking booking;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "flight_instance_id")
    private FlightInstance flightInstance;

    private String seatLabel;
    private String passengerName;

    /** Non-null while held;
     * NULL once released.
     * Part of the unique constraint.
     */
    private String activeSeatLabel;
}
