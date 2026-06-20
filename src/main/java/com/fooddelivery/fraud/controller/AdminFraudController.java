package com.fooddelivery.fraud.controller;

import com.fooddelivery.common.response.ApiResponse;
import com.fooddelivery.fraud.entity.DailyFraudMetrics;
import com.fooddelivery.fraud.service.FraudAdminService;
import com.fooddelivery.orders.dto.OrderResponse;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.security.Principal;
import java.util.List;

@RestController
@RequestMapping("/api/admin/fraud")
@Validated
public class AdminFraudController {

    private final FraudAdminService service;

    public AdminFraudController(FraudAdminService service) {
        this.service = service;
    }

    @GetMapping("/queue")
    public ResponseEntity<ApiResponse<List<OrderResponse>>> getFraudQueue() {
        List<OrderResponse> queue = service.getFraudQueue();
        return ResponseEntity.ok(ApiResponse.success("Fraud review queue retrieved successfully", queue));
    }

    @PostMapping("/cases/{orderId}/approve")
    public ResponseEntity<ApiResponse<OrderResponse>> approveOrder(
            @PathVariable String orderId,
            @Valid @RequestBody OverrideRequest request,
            Principal principal) {
        OrderResponse approved = service.approveOrder(orderId, principal.getName(), request.getReason());
        return ResponseEntity.ok(ApiResponse.success("Order approved and verification hold cleared", approved));
    }

    @PostMapping("/cases/{orderId}/reject")
    public ResponseEntity<ApiResponse<OrderResponse>> rejectOrder(
            @PathVariable String orderId,
            @Valid @RequestBody OverrideRequest request,
            Principal principal) {
        OrderResponse rejected = service.rejectOrder(orderId, principal.getName(), request.getReason());
        return ResponseEntity.ok(ApiResponse.success("Order rejected and hold released", rejected));
    }

    @PostMapping("/users/{userId}/restrict")
    public ResponseEntity<ApiResponse<Void>> restrictUser(
            @PathVariable String userId,
            @Valid @RequestBody RestrictRequest request,
            Principal principal) {
        service.restrictUser(userId, principal.getName(), request.getRestrictionTypes(), request.getReason(), request.getDurationDays());
        return ResponseEntity.ok(ApiResponse.success("User restriction matrix updated successfully"));
    }

    @GetMapping("/analytics/daily")
    public ResponseEntity<ApiResponse<DailyFraudMetrics>> getDailyMetrics(
            @RequestParam(required = false) String date) {
        DailyFraudMetrics metrics = service.getDailyMetrics(date);
        return ResponseEntity.ok(ApiResponse.success("Daily fraud metrics retrieved successfully", metrics));
    }

    public static class OverrideRequest {
        @NotBlank(message = "Override reason is mandatory")
        @Size(min = 10, message = "Override reason must be at least 10 characters long")
        private String reason;

        public String getReason() {
            return reason;
        }

        public void setReason(String reason) {
            this.reason = reason;
        }
    }

    public static class RestrictRequest {
        private List<String> restrictionTypes;
        
        @NotBlank(message = "Reason is mandatory")
        private String reason;
        
        @Min(value = 1, message = "Duration must be at least 1 day")
        private int durationDays;

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

        public int getDurationDays() {
            return durationDays;
        }

        public void setDurationDays(int durationDays) {
            this.durationDays = durationDays;
        }
    }
}
