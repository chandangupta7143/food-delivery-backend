package com.fooddelivery.surge.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;

public class AdminSurgeOverrideRequest {

    @NotBlank(message = "h3Index is mandatory")
    private String h3Index;

    @Min(value = 1, message = "targetMultiplier must be at least 1.0")
    private double targetMultiplier;

    @Min(value = 1, message = "durationMinutes must be at least 1 minute")
    private int durationMinutes;

    @NotBlank(message = "reason is mandatory")
    private String reason;

    // Getters and Setters

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

    public int getDurationMinutes() {
        return durationMinutes;
    }

    public void setDurationMinutes(int durationMinutes) {
        this.durationMinutes = durationMinutes;
    }

    public String getReason() {
        return reason;
    }

    public void setReason(String reason) {
        this.reason = reason;
    }
}
