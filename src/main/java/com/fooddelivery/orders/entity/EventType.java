package com.fooddelivery.orders.entity;

/**
 * Represents the type of event that triggered an order status change.
 */
public enum EventType {
    SYSTEM_EVENT,
    USER_ACTION,
    VENDOR_ACTION,
    ADMIN_OVERRIDE,
    DELIVERY_PARTNER_ACTION
}
