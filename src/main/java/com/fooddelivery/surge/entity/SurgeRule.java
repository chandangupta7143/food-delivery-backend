package com.fooddelivery.surge.entity;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.LocalDateTime;

@Document(collection = "surge_rules")
public class SurgeRule {

    @Id
    private String id;

    @Indexed(unique = true)
    private String zoneName; // e.g., "Noida", "Delhi_NCR"

    private double thresholdSurge; // Pressure threshold where surge begins (e.g., 40.0)
    private double scaleFactor; // Used to scale multiplier calculation (e.g., 60.0)
    private double maxMultiplier; // Hard limit multiplier (e.g., 3.0)
    private double baseMultiplier; // Standard multiplier (e.g., 1.0)
    
    private double demandWeight; // e.g., 0.40
    private double supplyWeight; // e.g., 0.40
    private double pressureWeight; // e.g., 0.20
    
    private boolean isActive;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    public SurgeRule() {
        this.createdAt = LocalDateTime.now();
        this.updatedAt = LocalDateTime.now();
        this.isActive = true;
        this.thresholdSurge = 40.0;
        this.scaleFactor = 60.0;
        this.maxMultiplier = 3.0;
        this.baseMultiplier = 1.0;
        this.demandWeight = 0.40;
        this.supplyWeight = 0.40;
        this.pressureWeight = 0.20;
    }

    // Getters and Setters

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getZoneName() {
        return zoneName;
    }

    public void setZoneName(String zoneName) {
        this.zoneName = zoneName;
    }

    public double getThresholdSurge() {
        return thresholdSurge;
    }

    public void setThresholdSurge(double thresholdSurge) {
        this.thresholdSurge = thresholdSurge;
    }

    public double getScaleFactor() {
        return scaleFactor;
    }

    public void setScaleFactor(double scaleFactor) {
        this.scaleFactor = scaleFactor;
    }

    public double getMaxMultiplier() {
        return maxMultiplier;
    }

    public void setMaxMultiplier(double maxMultiplier) {
        this.maxMultiplier = maxMultiplier;
    }

    public double getBaseMultiplier() {
        return baseMultiplier;
    }

    public void setBaseMultiplier(double baseMultiplier) {
        this.baseMultiplier = baseMultiplier;
    }

    public double getDemandWeight() {
        return demandWeight;
    }

    public void setDemandWeight(double demandWeight) {
        this.demandWeight = demandWeight;
    }

    public double getSupplyWeight() {
        return supplyWeight;
    }

    public void setSupplyWeight(double supplyWeight) {
        this.supplyWeight = supplyWeight;
    }

    public double getPressureWeight() {
        return pressureWeight;
    }

    public void setPressureWeight(double pressureWeight) {
        this.pressureWeight = pressureWeight;
    }

    public boolean isActive() {
        return isActive;
    }

    public void setActive(boolean active) {
        isActive = active;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(LocalDateTime updatedAt) {
        this.updatedAt = updatedAt;
    }
}
