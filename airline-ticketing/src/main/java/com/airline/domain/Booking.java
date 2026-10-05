package com.airline.domain;

import jakarta.persistence.*;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Entity for booking PNR
 *
 * @author Loknath Kumar
 * @version 1.0
 * @since 2026-10-04
 */
@Entity
@Table(name = "booking")
@Getter
@Setter
@NoArgsConstructor
public class Booking {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String pnr;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "flight_instance_id")
    private FlightInstance flightInstance;

    @Enumerated(EnumType.STRING)
    private BookingStatus status;

    private int passengerCount;
    private Instant createdAt;
    private Instant cancelledAt;

    @Version
    private Long version;

    @OneToMany(mappedBy = "booking", cascade = CascadeType.ALL)
    @OrderBy("id")
    private List<BookingSeat> seats = new ArrayList<>();

    public void addSeat(String seatLabel, String passengerName) {
        BookingSeat bookingSeat = new BookingSeat();
        bookingSeat.setBooking(this);
        bookingSeat.setFlightInstance(this.flightInstance);
        bookingSeat.setSeatLabel(seatLabel);
        bookingSeat.setActiveSeatLabel(seatLabel);
        bookingSeat.setPassengerName(passengerName);
        seats.add(bookingSeat);
    }

    /** Marks the booking cancelled and releases every seat (frees the unique slot). */
    public void cancel(Instant now) {
        this.status = BookingStatus.CANCELLED;
        this.cancelledAt = now;
        seats.forEach(bookingSeat -> bookingSeat.setActiveSeatLabel(null));
    }
}
