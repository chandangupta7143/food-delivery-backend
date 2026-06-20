package com.fooddelivery.fraud.entity;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.LocalDateTime;

@Document(collection = "fraud_audit_logs")
public class FraudAuditLog {

    @Id
    private String id;

    @Indexed
    private String orderId;

    @Indexed
    private String userId;

    private String action; // APPROVE, REJECT
    private String adminEmail;
    private String notes;
    private LocalDateTime timestamp;

    public FraudAuditLog() {
        this.timestamp = LocalDateTime.now();
    }

    public FraudAuditLog(String orderId, String userId, String action, String adminEmail, String notes) {
        this();
        this.orderId = orderId;
        this.userId = userId;
        this.action = action;
        this.adminEmail = adminEmail;
        this.notes = notes;
    }

    // Getters and Setters

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getOrderId() {
        return orderId;
    }

    public void setOrderId(String orderId) {
        this.orderId = orderId;
    }

    public String getUserId() {
        return userId;
    }

    public void setUserId(String userId) {
        this.userId = userId;
    }

    public String getAction() {
        return action;
    }

    public void setAction(String action) {
        this.action = action;
    }

    public String getAdminEmail() {
        return adminEmail;
    }

    public void setAdminEmail(String adminEmail) {
        this.adminEmail = adminEmail;
    }

    public String getNotes() {
        return notes;
    }

    public void setNotes(String notes) {
        this.notes = notes;
    }

    public LocalDateTime getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(LocalDateTime timestamp) {
        this.timestamp = timestamp;
    }
}
