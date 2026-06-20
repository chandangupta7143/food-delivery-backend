package com.fooddelivery.fraud.entity;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.LocalDateTime;

@Document(collection = "daily_fraud_metrics")
public class DailyFraudMetrics {

    @Id
    private String id; // Format: "yyyy-MM-dd"

    private long totalOrdersProcessed;
    private long totalAutoRejected;
    private long totalHoldForReview;
    private long totalPromoBlocked;
    private LocalDateTime lastUpdatedAt;

    public DailyFraudMetrics() {
        this.lastUpdatedAt = LocalDateTime.now();
    }

    public DailyFraudMetrics(String dateStr) {
        this();
        this.id = dateStr;
    }

    // Getters and Setters

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public long getTotalOrdersProcessed() {
        return totalOrdersProcessed;
    }

    public void setTotalOrdersProcessed(long totalOrdersProcessed) {
        this.totalOrdersProcessed = totalOrdersProcessed;
    }

    public long getTotalAutoRejected() {
        return totalAutoRejected;
    }

    public void setTotalAutoRejected(long totalAutoRejected) {
        this.totalAutoRejected = totalAutoRejected;
    }

    public long getTotalHoldForReview() {
        return totalHoldForReview;
    }

    public void setTotalHoldForReview(long totalHoldForReview) {
        this.totalHoldForReview = totalHoldForReview;
    }

    public long getTotalPromoBlocked() {
        return totalPromoBlocked;
    }

    public void setTotalPromoBlocked(long totalPromoBlocked) {
        this.totalPromoBlocked = totalPromoBlocked;
    }

    public LocalDateTime getLastUpdatedAt() {
        return lastUpdatedAt;
    }

    public void setLastUpdatedAt(LocalDateTime lastUpdatedAt) {
        this.lastUpdatedAt = lastUpdatedAt;
    }
}
