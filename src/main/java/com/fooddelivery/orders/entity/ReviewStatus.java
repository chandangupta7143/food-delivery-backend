package com.fooddelivery.orders.entity;

/**
 * Represents the fraud review status of an Order.
 */
public enum ReviewStatus {
    PASSED,
    FLAGGED,
    MANUAL_REVIEW,
    REJECTED
}
