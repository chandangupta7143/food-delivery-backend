package com.fooddelivery.orders.dto;

import com.fooddelivery.orders.entity.ReviewStatus;
import jakarta.validation.constraints.NotNull;

public class FraudReviewRequest {

    @NotNull(message = "Review status decision is required")
    private ReviewStatus decision;

    private String adminNotes;

    public FraudReviewRequest() {
    }

    public FraudReviewRequest(ReviewStatus decision, String adminNotes) {
        this.decision = decision;
        this.adminNotes = adminNotes;
    }

    public ReviewStatus getDecision() {
        return decision;
    }

    public void setDecision(ReviewStatus decision) {
        this.decision = decision;
    }

    public String getAdminNotes() {
        return adminNotes;
    }

    public void setAdminNotes(String adminNotes) {
        this.adminNotes = adminNotes;
    }
}
