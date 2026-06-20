package com.fooddelivery.common.exception;

/**
 * Thrown when login credentials (email or password) are invalid.
 */
public class InvalidCredentialsException extends RuntimeException {

    public InvalidCredentialsException(String message) {
        super(message);
    }
}
