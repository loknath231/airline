package com.airline.exception;

import static org.junit.jupiter.api.Assertions.*;

import com.airline.domain.Booking;
import com.airline.web.dto.ApiError;
import jakarta.validation.ConstraintViolationException;
import java.sql.SQLException;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.Set;
import org.junit.jupiter.api.Test;
import org.springframework.dao.CannotAcquireLockException;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.ResponseEntity;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.orm.ObjectOptimisticLockingFailureException;
import org.springframework.web.HttpRequestMethodNotSupportedException;

/** Handlers that are hard to reach through the API are exercised directly. */
class GlobalExceptionHandlerTest {
    private static final Instant NOW = Instant.parse("2026-10-04T10:00:00Z");

    private final GlobalExceptionHandler handler = new GlobalExceptionHandler(Clock.fixed(NOW, ZoneOffset.UTC));
    private final MockHttpServletRequest request = new MockHttpServletRequest("POST", "/api/v1/bookings");

    private static void assertError(ResponseEntity<ApiError> res, int status, String message) {
        assertEquals(status, res.getStatusCode().value());
        ApiError body = res.getBody();
        assertNotNull(body);
        assertEquals(status, body.status());
        assertEquals(message, body.message());
        assertEquals("/api/v1/bookings", body.path());
        assertEquals(NOW, body.timestamp());
        assertTrue(body.details().isEmpty());
    }

    @Test
    void constraintViolation_is400() {
        assertError(
                handler.constraint(new ConstraintViolationException("bad value", Set.of()), request), 400, "bad value");
    }

    @Test
    void dataIntegrityViolation_is409() {
        var e = new DataIntegrityViolationException("dup", new SQLException("uq_active_seat"));
        assertError(
                handler.integrity(e, request), 409, "Request conflicts with existing data (e.g. seat already booked)");
    }

    @Test
    void optimisticLockFailure_is409() {
        var e = new ObjectOptimisticLockingFailureException(Booking.class, 1L);
        assertError(handler.optimistic(e, request), 409, "Resource was modified concurrently, please retry");
    }

    @Test
    void lockTimeout_is409_retryable() {
        var e = new CannotAcquireLockException("timeout", new SQLException("Timeout trying to lock table"));
        assertError(handler.lockTimeout(e, request), 409, "Flight is busy with other bookings, please retry");
    }

    @Test
    void springErrorResponse_keepsItsStatusAndHeaders() {
        var e = new HttpRequestMethodNotSupportedException("DELETE", Set.of("GET"));
        ResponseEntity<ApiError> res = handler.generic(e, request);
        assertEquals(405, res.getStatusCode().value());
        assertEquals("Method 'DELETE' is not supported.", res.getBody().message());
        assertEquals("GET", res.getHeaders().getFirst("Allow"));
    }

    @Test
    void unexpectedException_is500_withGenericMessage() {
        assertError(handler.generic(new IllegalStateException("secret detail"), request), 500, "Unexpected error");
    }
}
