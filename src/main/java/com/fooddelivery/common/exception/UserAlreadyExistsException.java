package com.fooddelivery.common.exception;

/**
 * Thrown when a registration attempt uses an email that already exists.
 */
public class UserAlreadyExistsException extends RuntimeException {

    public UserAlreadyExistsException(String message) {
        super(message);
    }
}
