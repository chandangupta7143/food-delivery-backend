package com.fooddelivery.delivery.entity;

import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.Version;
import org.springframework.data.mongodb.core.geo.GeoJsonPoint;
import org.springframework.data.mongodb.core.index.CompoundIndex;
import org.springframework.data.mongodb.core.index.CompoundIndexes;
import org.springframework.data.mongodb.core.index.GeoSpatialIndexType;
import org.springframework.data.mongodb.core.index.GeoSpatialIndexed;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.LocalDateTime;

@Document(collection = "delivery_partners")
@CompoundIndexes({
        @CompoundIndex(name = "idx_status_location_update", def = "{'status': 1, 'lastLocationUpdateTime': -1}")
})
public class DeliveryPartner {

    @Id
    private String id;

    @Indexed(unique = true)
    private String userId;

    private VehicleType vehicleType;

    @GeoSpatialIndexed(type = GeoSpatialIndexType.GEO_2DSPHERE)
    private GeoJsonPoint currentLocation;

    private LocalDateTime lastLocationUpdateTime;

    @Indexed
    private DeliveryPartnerStatus status;

    private String currentOrderId;

    private int dailyDeliveryCount;

    private double acceptanceRate;

    private double averageDeliveryTimeMinutes;

    private double rating;

    private LocalDateTime lastActiveTime;

    // Analytics
    private int totalAssignments;
    private int totalAccepted;
    private int totalRejected;
    private int totalTimeouts;

    private int consecutiveRejections;

    @Version
    private Long version;

    public DeliveryPartner() {
        this.status = DeliveryPartnerStatus.OFFLINE;
        this.dailyDeliveryCount = 0;
        this.acceptanceRate = 100.0;
        this.averageDeliveryTimeMinutes = 25.0;
        this.rating = 5.0;
        this.totalAssignments = 0;
        this.totalAccepted = 0;
        this.totalRejected = 0;
        this.totalTimeouts = 0;
        this.consecutiveRejections = 0;
        this.lastActiveTime = LocalDateTime.now();
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

    public GeoJsonPoint getCurrentLocation() {
        return currentLocation;
    }

    public void setCurrentLocation(GeoJsonPoint currentLocation) {
        this.currentLocation = currentLocation;
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

    public LocalDateTime getLastActiveTime() {
        return lastActiveTime;
    }

    public void setLastActiveTime(LocalDateTime lastActiveTime) {
        this.lastActiveTime = lastActiveTime;
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

    public int getConsecutiveRejections() {
        return consecutiveRejections;
    }

    public void setConsecutiveRejections(int consecutiveRejections) {
        this.consecutiveRejections = consecutiveRejections;
    }

    public Long getVersion() {
        return version;
    }

    public void setVersion(Long version) {
        this.version = version;
    }
}
