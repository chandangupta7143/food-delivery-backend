package com.fooddelivery.recommendations.service;

import com.fooddelivery.recommendations.dto.RecommendationQueryRequest;
import com.fooddelivery.recommendations.dto.RecommendationQueryResponse;

public interface RecommendationService {

    /**
     * Retrieves personalized, context-aware, and diverse recommendations for the user.
     */
    RecommendationQueryResponse calculateRecommendations(RecommendationQueryRequest request, String userEmail);

    /**
     * Logs recommendation click event for CTR analysis.
     */
    void logClick(String eventId, String restaurantId);

    /**
     * Attributes order creation to a recommendation event for CVR analysis.
     */
    void attributeOrderConversion(String recommendationEventId, String orderId, String restaurantId, double revenue);

    /**
     * Updates user taste profile (cuisines, loyalties) upon successful order completion.
     */
    void updateUserAffinities(String userId, String restaurantId, String cuisine, double orderAmount);
}
