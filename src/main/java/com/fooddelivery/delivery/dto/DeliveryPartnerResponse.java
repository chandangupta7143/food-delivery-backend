package com.fooddelivery.delivery.dto;

import com.fooddelivery.delivery.entity.DeliveryPartnerStatus;
import com.fooddelivery.delivery.entity.VehicleType;

import java.time.LocalDateTime;

public class DeliveryPartnerResponse {

    private String id;
    private String userId;
    private VehicleType vehicleType;
    private Double latitude;
    private Double longitude;
    private LocalDateTime lastLocationUpdateTime;
    private DeliveryPartnerStatus status;
    private String currentOrderId;
    private int dailyDeliveryCount;
    private double acceptanceRate;
    private double averageDeliveryTimeMinutes;
    private double rating;
    private int totalAssignments;
    private int totalAccepted;
    private int totalRejected;
    private int totalTimeouts;

    public DeliveryPartnerResponse() {
    }

    // Getters and Setters

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getUserId() {
        return userId;
    }

    public void setUserId(String userId) {
        this.userId = userId;
    }

    public VehicleType getVehicleType() {
        return vehicleType;
    }

    public void setVehicleType(VehicleType vehicleType) {
        this.vehicleType = vehicleType;
    }

    public Double getLatitude() {
        return latitude;
    }

    public void setLatitude(Double latitude) {
        this.latitude = latitude;
    }

    public Double getLongitude() {
        return longitude;
    }

    public void setLongitude(Double longitude) {
        this.longitude = longitude;
    }

    public LocalDateTime getLastLocationUpdateTime() {
        return lastLocationUpdateTime;
    }

    public void setLastLocationUpdateTime(LocalDateTime lastLocationUpdateTime) {
        this.lastLocationUpdateTime = lastLocationUpdateTime;
    }

    public DeliveryPartnerStatus getStatus() {
        return status;
    }

    public void setStatus(DeliveryPartnerStatus status) {
        this.status = status;
    }

    public String getCurrentOrderId() {
        return currentOrderId;
    }

    public void setCurrentOrderId(String currentOrderId) {
        this.currentOrderId = currentOrderId;
    }

    public int getDailyDeliveryCount() {
        return dailyDeliveryCount;
    }

    public void setDailyDeliveryCount(int dailyDeliveryCount) {
        this.dailyDeliveryCount = dailyDeliveryCount;
    }

    public double getAcceptanceRate() {
        return acceptanceRate;
    }

    public void setAcceptanceRate(double acceptanceRate) {
        this.acceptanceRate = acceptanceRate;
    }

    public double getAverageDeliveryTimeMinutes() {
        return averageDeliveryTimeMinutes;
    }

    public void setAverageDeliveryTimeMinutes(double averageDeliveryTimeMinutes) {
        this.averageDeliveryTimeMinutes = averageDeliveryTimeMinutes;
    }

    public double getRating() {
        return rating;
    }

    public void setRating(double rating) {
        this.rating = rating;
    }

    public int getTotalAssignments() {
        return totalAssignments;
    }

    public void setTotalAssignments(int totalAssignments) {
        this.totalAssignments = totalAssignments;
    }

    public int getTotalAccepted() {
        return totalAccepted;
    }

    public void setTotalAccepted(int totalAccepted) {
        this.totalAccepted = totalAccepted;
    }

    public int getTotalRejected() {
        return totalRejected;
    }

    public void setTotalRejected(int totalRejected) {
        this.totalRejected = totalRejected;
    }

    public int getTotalTimeouts() {
        return totalTimeouts;
    }

    public void setTotalTimeouts(int totalTimeouts) {
        this.totalTimeouts = totalTimeouts;
    }
}
