package com.fooddelivery.orders.dto;

import com.fooddelivery.orders.entity.ReviewStatus;

import java.util.List;

public class FraudDetailsResponse {

    private double riskScore;
    private List<String> fraudFlags;
    private ReviewStatus reviewStatus;
    private String adminReviewNotes;

    public FraudDetailsResponse() {
    }

    public FraudDetailsResponse(double riskScore, List<String> fraudFlags, ReviewStatus reviewStatus, String adminReviewNotes) {
        this.riskScore = riskScore;
        this.fraudFlags = fraudFlags;
        this.reviewStatus = reviewStatus;
        this.adminReviewNotes = adminReviewNotes;
    }

    // Getters and Setters

    public double getRiskScore() {
        return riskScore;
    }

    public void setRiskScore(double riskScore) {
        this.riskScore = riskScore;
    }

    public List<String> getFraudFlags() {
        return fraudFlags;
    }

    public void setFraudFlags(List<String> fraudFlags) {
        this.fraudFlags = fraudFlags;
    }

    public ReviewStatus getReviewStatus() {
        return reviewStatus;
    }

    public void setReviewStatus(ReviewStatus reviewStatus) {
        this.reviewStatus = reviewStatus;
    }

    public String getAdminReviewNotes() {
        return adminReviewNotes;
    }

    public void setAdminReviewNotes(String adminReviewNotes) {
        this.adminReviewNotes = adminReviewNotes;
    }
}
