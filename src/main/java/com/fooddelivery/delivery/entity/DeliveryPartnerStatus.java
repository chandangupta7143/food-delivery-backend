package com.fooddelivery.delivery.entity;

/**
 * Represents the availability and matching lifecycle state of a Delivery Partner.
 */
public enum DeliveryPartnerStatus {
    OFFLINE,
    ONLINE,
    BUSY,
    ON_DELIVERY,
    SUSPENDED
}
