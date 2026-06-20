package com.fooddelivery.common.exception;

/**
 * Generic exception thrown when a requested resource (Restaurant, Order, etc.) is not found.
 */
public class ResourceNotFoundException extends RuntimeException {
    public ResourceNotFoundException(String message) {
        super(message);
    }
}
