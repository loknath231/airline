package com.airline.exception;

import com.airline.web.dto.ApiError;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.ConstraintViolationException;
import java.time.Clock;
import java.time.Instant;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.dao.PessimisticLockingFailureException;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.orm.ObjectOptimisticLockingFailureException;
import org.springframework.web.ErrorResponse;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;

/**
 * Thown generally during invalid request like wrong date etc.
 *
 * @author Loknath Kumar
 * @version 1.0
 * @since 2026-10-04
 */
@RestControllerAdvice
@RequiredArgsConstructor
@Slf4j
public class GlobalExceptionHandler {
    private final Clock clock;

    @ExceptionHandler(ResourceNotFoundException.class)
    ResponseEntity<ApiError> notFound(ResourceNotFoundException e, HttpServletRequest r) {
        return build(HttpStatus.NOT_FOUND, e.getMessage(), r, List.of());
    }

    @ExceptionHandler(ConflictException.class)
    ResponseEntity<ApiError> conflict(ConflictException e, HttpServletRequest r) {
        return build(HttpStatus.CONFLICT, e.getMessage(), r, List.of());
    }

    @ExceptionHandler(InvalidRequestException.class)
    ResponseEntity<ApiError> invalid(InvalidRequestException e, HttpServletRequest r) {
        return build(HttpStatus.BAD_REQUEST, e.getMessage(), r, List.of());
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    ResponseEntity<ApiError> validation(MethodArgumentNotValidException e, HttpServletRequest r) {
        List<String> details = e.getBindingResult().getFieldErrors().stream()
                .map(f -> f.getField() + ": " + f.getDefaultMessage())
                .toList();
        return build(HttpStatus.BAD_REQUEST, "Validation failed", r, details);
    }

    @ExceptionHandler(ConstraintViolationException.class)
    ResponseEntity<ApiError> constraint(ConstraintViolationException e, HttpServletRequest r) {
        return build(HttpStatus.BAD_REQUEST, e.getMessage(), r, List.of());
    }

    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    ResponseEntity<ApiError> typeMismatch(MethodArgumentTypeMismatchException e, HttpServletRequest r) {
        return build(HttpStatus.BAD_REQUEST, "Invalid value for parameter '" + e.getName() + "'", r, List.of());
    }

    @ExceptionHandler(MissingServletRequestParameterException.class)
    ResponseEntity<ApiError> missingParam(MissingServletRequestParameterException e, HttpServletRequest r) {
        return build(HttpStatus.BAD_REQUEST, "Missing parameter '" + e.getParameterName() + "'", r, List.of());
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    ResponseEntity<ApiError> unreadable(HttpMessageNotReadableException e, HttpServletRequest r) {
        return build(HttpStatus.BAD_REQUEST, "Malformed JSON request body", r, List.of());
    }

    /** Safety net: UNIQUE(flight_instance_id, active_seat_label) fired. */
    @ExceptionHandler(DataIntegrityViolationException.class)
    ResponseEntity<ApiError> integrity(DataIntegrityViolationException e, HttpServletRequest r) {
        log.warn("Data integrity violation: {}", e.getMostSpecificCause().getMessage());
        return build(
                HttpStatus.CONFLICT, "Request conflicts with existing data (e.g. seat already booked)", r, List.of());
    }

    @ExceptionHandler(ObjectOptimisticLockingFailureException.class)
    ResponseEntity<ApiError> optimistic(ObjectOptimisticLockingFailureException e, HttpServletRequest r) {
        return build(HttpStatus.CONFLICT, "Resource was modified concurrently, please retry", r, List.of());
    }

    /** The flight row lock could not be acquired within LOCK_TIMEOUT: the flight is busy, the client may retry. */
    @ExceptionHandler(PessimisticLockingFailureException.class)
    ResponseEntity<ApiError> lockTimeout(PessimisticLockingFailureException e, HttpServletRequest r) {
        log.warn(
                "Lock not acquired on {}: {}",
                r.getRequestURI(),
                e.getMostSpecificCause().getMessage());
        return build(HttpStatus.CONFLICT, "Flight is busy with other bookings, please retry", r, List.of());
    }

    @ExceptionHandler(Exception.class)
    ResponseEntity<ApiError> generic(Exception e, HttpServletRequest r) {
        // Spring MVC's own errors (unknown URL, wrong method, unsupported media type...) carry their status code
        if (e instanceof ErrorResponse er) {
            HttpStatus status = HttpStatus.valueOf(er.getStatusCode().value());
            String detail = er.getBody().getDetail();
            log.debug("{} on {}: {}", status.value(), r.getRequestURI(), detail);
            return build(status, detail != null ? detail : status.getReasonPhrase(), r, List.of(), er.getHeaders());
        }
        log.error("Unhandled error on {}", r.getRequestURI(), e);
        return build(HttpStatus.INTERNAL_SERVER_ERROR, "Unexpected error", r, List.of());
    }

    private ResponseEntity<ApiError> build(HttpStatus s, String msg, HttpServletRequest r, List<String> details) {
        return build(s, msg, r, details, HttpHeaders.EMPTY);
    }

    private ResponseEntity<ApiError> build(
            HttpStatus s, String msg, HttpServletRequest r, List<String> details, HttpHeaders headers) {
        return ResponseEntity.status(s)
                .headers(headers)
                .body(new ApiError(
                        Instant.now(clock), s.value(), s.getReasonPhrase(), msg, r.getRequestURI(), details));
    }
}
