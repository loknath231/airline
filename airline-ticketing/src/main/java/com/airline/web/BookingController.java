package com.airline.web;

import com.airline.service.BookingService;
import com.airline.web.dto.BookingResponse;
import com.airline.web.dto.CreateBookingRequest;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.net.URI;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

/**
 * Controller responsible flight booking
 *
 * @author Loknath Kumar
 * @version 1.0
 * @since 2026-10-04
 */
@RestController
@RequestMapping("/api/v1/bookings")
@RequiredArgsConstructor
@Tag(name = "Bookings")
public class BookingController {
    private final BookingService bookingService;

    /**
     * Books one or more seats on a flight instance (409 if any seat is already taken).
     *
     * @param createBookingRequest flight instance id and one seat per passenger
     * @return 201 with the booking and a {@code Location} header pointing to it
     */
    @PostMapping
    @Operation(summary = "Create a booking for one or more seats (409 if any seat is taken)")
    public ResponseEntity<BookingResponse> create(@Valid @RequestBody CreateBookingRequest createBookingRequest) {
        BookingResponse r = bookingService.create(createBookingRequest);
        return ResponseEntity.created(URI.create("/api/v1/bookings/" + r.pnr())).body(r);
    }

    /**
     * Returns a booking.
     *
     * @param pnr booking reference
     * @return the booking (404 if unknown)
     */
    @GetMapping("/{pnr}")
    @Operation(summary = "Get a booking by PNR")
    public BookingResponse get(@PathVariable String pnr) {
        return bookingService.get(pnr);
    }

    /**
     * Cancels a booking and releases its seats (409 if already cancelled or the flight has departed).
     *
     * @param pnr booking reference
     * @return the cancelled booking
     */
    @PostMapping("/{pnr}/cancel")
    @Operation(summary = "Cancel a booking and release its seats")
    public BookingResponse cancel(@PathVariable String pnr) {
        return bookingService.cancel(pnr);
    }
}
