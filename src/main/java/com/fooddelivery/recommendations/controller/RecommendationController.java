package com.fooddelivery.recommendations.controller;

import com.fooddelivery.common.response.ApiResponse;
import com.fooddelivery.recommendations.dto.RecommendationQueryRequest;
import com.fooddelivery.recommendations.dto.RecommendationQueryResponse;
import com.fooddelivery.recommendations.service.RecommendationService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.security.Principal;

@RestController
@RequestMapping("/api/recommendations")
@Validated
public class RecommendationController {

    private final RecommendationService recommendationService;

    public RecommendationController(RecommendationService recommendationService) {
        this.recommendationService = recommendationService;
    }

    @PostMapping("/discover")
    public ResponseEntity<ApiResponse<RecommendationQueryResponse>> getRecommendations(
            @Valid @RequestBody RecommendationQueryRequest request,
            Principal principal) {
        RecommendationQueryResponse response = recommendationService.calculateRecommendations(request, principal.getName());
        return ResponseEntity.ok(ApiResponse.success("Recommendations retrieved successfully", response));
    }

    @PostMapping("/event/click")
    public ResponseEntity<ApiResponse<Void>> logRecommendationClick(
            @RequestParam String eventId,
            @RequestParam String restaurantId) {
        recommendationService.logClick(eventId, restaurantId);
        return ResponseEntity.ok(ApiResponse.success("Recommendation click logged successfully"));
    }
}
