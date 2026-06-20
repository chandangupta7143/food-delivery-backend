package com.fooddelivery.orders.entity;

/**
 * Represents the current lifecycle state of an Order.
 */
public enum OrderStatus {
    CREATED,
    PENDING_REVIEW,
    RESTAURANT_ACCEPTED,
    PREPARING,
    READY_FOR_PICKUP,
    OUT_FOR_DELIVERY,
    DELIVERED,
    CANCELLED,
    REJECTED
}
