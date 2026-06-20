package com.fooddelivery.surge.dto;

import java.time.LocalDateTime;

public class AdminSurgeOverrideResponse {

    private String overrideId;
    private String h3Index;
    private double targetMultiplier;
    private LocalDateTime expiresAt;
    private String status;

    public AdminSurgeOverrideResponse() {
    }

    public AdminSurgeOverrideResponse(String overrideId, String h3Index, double targetMultiplier, LocalDateTime expiresAt, String status) {
        this.overrideId = overrideId;
        this.h3Index = h3Index;
        this.targetMultiplier = targetMultiplier;
        this.expiresAt = expiresAt;
        this.status = status;
    }

    // Getters and Setters

    public String getOverrideId() {
        return overrideId;
    }

    public void setOverrideId(String overrideId) {
        this.overrideId = overrideId;
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

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }
}
