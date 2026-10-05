package com.airline.exception;

/**
 * Thown when two booking coming for same seat in same flight for same date
 *
 * @author Loknath Kumar
 * @version 1.0
 * @since 2026-10-04
 */
public class ConflictException extends RuntimeException {
    public ConflictException(String message) {
        super(message);
    }
}
