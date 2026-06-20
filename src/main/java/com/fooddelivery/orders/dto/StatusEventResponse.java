package com.fooddelivery.orders.dto;

import com.fooddelivery.orders.entity.EventType;
import com.fooddelivery.orders.entity.OrderStatus;

import java.time.LocalDateTime;

public class StatusEventResponse {

    private OrderStatus status;
    private EventType eventType;
    private LocalDateTime timestamp;
    private String actorId;
    private String actorType;

    public StatusEventResponse() {
    }

    public StatusEventResponse(OrderStatus status, EventType eventType, LocalDateTime timestamp, String actorId, String actorType) {
        this.status = status;
        this.eventType = eventType;
        this.timestamp = timestamp;
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
