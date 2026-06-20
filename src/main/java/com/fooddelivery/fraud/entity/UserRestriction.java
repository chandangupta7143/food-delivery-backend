package com.fooddelivery.fraud.entity;

import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Document(collection = "user_restrictions")
public class UserRestriction {

    @Id
    private String id;

    @Indexed(unique = true)
    private String userId;

    private List<String> restrictionTypes = new ArrayList<>(); // BLOCK_COUPONS, BLOCK_REFUNDS, BLOCK_ORDERING
    private String reason;

    @Indexed(expireAfterSeconds = 0) // TTL index: expires at the timestamp stored in expiresAt
    private LocalDateTime expiresAt;

    @CreatedDate
    private LocalDateTime createdAt;
    private String createdBy;

    public UserRestriction() {
        this.createdAt = LocalDateTime.now();
    }

    public UserRestriction(String userId, List<String> restrictionTypes, String reason, LocalDateTime expiresAt, String createdBy) {
        this();
        this.userId = userId;
        this.restrictionTypes = restrictionTypes != null ? restrictionTypes : new ArrayList<>();
        this.reason = reason;
        this.expiresAt = expiresAt;
        this.createdBy = createdBy;
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

    public List<String> getRestrictionTypes() {
        return restrictionTypes;
    }

    public void setRestrictionTypes(List<String> restrictionTypes) {
        this.restrictionTypes = restrictionTypes;
    }

    public String getReason() {
        return reason;
    }

    public void setReason(String reason) {
        this.reason = reason;
    }

    public LocalDateTime getExpiresAt() {
        return expiresAt;
    }

    public void setExpiresAt(LocalDateTime expiresAt) {
        this.expiresAt = expiresAt;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public String getCreatedBy() {
        return createdBy;
    }

    public void setCreatedBy(String createdBy) {
        this.createdBy = createdBy;
    }
}
