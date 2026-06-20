package com.fooddelivery.orders.entity;

import java.util.ArrayList;
import java.util.List;

/**
 * Embedded document representing fraud prevention evaluations, flags, and rule snapshots.
 */
public class FraudDetails {

    private double riskScore;
    private List<String> fraudFlags = new ArrayList<>();
    private List<TriggeredRule> triggeredRules = new ArrayList<>();
    private String ruleVersion;
    private ReviewStatus reviewStatus;
    private String adminReviewNotes;
    private String adminOverrideReason;

    public FraudDetails() {
        this.reviewStatus = ReviewStatus.PASSED;
        this.riskScore = 0.0;
        this.ruleVersion = "FRAUD_RULESET_V1";
    }

    public FraudDetails(double riskScore, List<String> fraudFlags, ReviewStatus reviewStatus, String adminReviewNotes) {
        this();
        this.riskScore = riskScore;
        this.fraudFlags = fraudFlags != null ? fraudFlags : new ArrayList<>();
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

    public List<TriggeredRule> getTriggeredRules() {
        return triggeredRules;
    }

    public void setTriggeredRules(List<TriggeredRule> triggeredRules) {
        this.triggeredRules = triggeredRules;
    }

    public String getRuleVersion() {
        return ruleVersion;
    }

    public void setRuleVersion(String ruleVersion) {
        this.ruleVersion = ruleVersion;
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

    public String getAdminOverrideReason() {
        return adminOverrideReason;
    }

    public void setAdminOverrideReason(String adminOverrideReason) {
        this.adminOverrideReason = adminOverrideReason;
    }
}
