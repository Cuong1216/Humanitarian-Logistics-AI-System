package com.humanitarian.logistics.core.exception;

/**
 * Custom exception class representing a "Resource Not Found" scenario.
 * It will be handled globally by the GlobalExceptionHandler.
 */
public class ResourceNotFoundException extends RuntimeException {
    
    public ResourceNotFoundException(String message) {
        super(message);
    }
}
