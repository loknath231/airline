package com.airline.exception;

/**
 * Thown generally during invalid request like wrong date etc.
 *
 * @author Loknath Kumar
 * @version 1.0
 * @since 2026-10-04
 */
public class InvalidRequestException extends RuntimeException {
    public InvalidRequestException(String message) {
        super(message);
    }
}
