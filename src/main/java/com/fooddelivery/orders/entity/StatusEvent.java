package com.fooddelivery.orders.entity;

import java.time.LocalDateTime;

/**
 * Embedded document representing a single status change event.
 */
public class StatusEvent {

    private OrderStatus status;
    private EventType eventType;
    private LocalDateTime timestamp;
    private String actorId;
    private String actorType; // e.g. USER, RESTAURANT, SYSTEM, ADMIN, DELIVERY_PARTNER

    public StatusEvent() {
        this.timestamp = LocalDateTime.now();
    }

    public StatusEvent(OrderStatus status, EventType eventType, LocalDateTime timestamp, String actorId, String actorType) {
        this.status = status;
        this.eventType = eventType;
        this.timestamp = timestamp != null ? timestamp : LocalDateTime.now();
        this.actorId = actorId;
        this.actorType = actorType;
    }

    // Getters and Setters

    public OrderStatus getStatus() {
        return status;
    }

    public void setStatus(OrderStatus status) {
        this.status = status;
    }

    public EventType getEventType() {
        return eventType;
    }

    public void setEventType(EventType eventType) {
        this.eventType = eventType;
    }

    public LocalDateTime getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(LocalDateTime timestamp) {
        this.timestamp = timestamp;
    }

    public String getActorId() {
        return actorId;
    }

    public void setActorId(String actorId) {
        this.actorId = actorId;
    }

    public String getActorType() {
        return actorType;
    }

    public void setActorType(String actorType) {
        this.actorType = actorType;
    }
}
