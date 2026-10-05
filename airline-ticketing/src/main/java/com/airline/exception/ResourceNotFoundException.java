package com.airline.exception;

/**
 * Thown if resource not found
 *
 * @author Loknath Kumar
 * @version 1.0
 * @since 2026-10-04
 */
public class ResourceNotFoundException extends RuntimeException {
    public ResourceNotFoundException(String message) {
        super(message);
    }
}
