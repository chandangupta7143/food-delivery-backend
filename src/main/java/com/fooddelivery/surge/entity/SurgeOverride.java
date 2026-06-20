package com.fooddelivery.surge.entity;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.LocalDateTime;

@Document(collection = "surge_overrides")
public class SurgeOverride {

    @Id
    private String id;

    @Indexed
    private String h3Index; // Represents the target grid cell

    private double targetMultiplier;
    private LocalDateTime expiresAt;
    private String reason;
    private String createdBy;
    private LocalDateTime createdAt;
    private boolean isActive;

    public SurgeOverride() {
        this.createdAt = LocalDateTime.now();
        this.isActive = true;
    }

    public SurgeOverride(String h3Index, double targetMultiplier, LocalDateTime expiresAt, String reason, String createdBy) {
        this();
        this.h3Index = h3Index;
        this.targetMultiplier = targetMultiplier;
        this.expiresAt = expiresAt;
        this.reason = reason;
        this.createdBy = createdBy;
    }

    // Getters and Setters

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getH3Index() {
        return h3Index;
    }

    public void setH3Index(String h3Index) {
        this.h3Index = h3Index;
    }

    public double getTargetMultiplier() {
        return targetMultiplier;
    }

    public void setTargetMultiplier(double targetMultiplier) {
        this.targetMultiplier = targetMultiplier;
    }

    public LocalDateTime getExpiresAt() {
        return expiresAt;
    }

    public void setExpiresAt(LocalDateTime expiresAt) {
        this.expiresAt = expiresAt;
    }

    public String getReason() {
        return reason;
    }

    public void setReason(String reason) {
        this.reason = reason;
    }

    public String getCreatedBy() {
        return createdBy;
    }

    public void setCreatedBy(String createdBy) {
        this.createdBy = createdBy;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public boolean isActive() {
        return isActive;
    }

    public void setActive(boolean active) {
        isActive = active;
    }
}
