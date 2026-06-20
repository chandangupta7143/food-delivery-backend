package com.fooddelivery.delivery.controller;

import com.fooddelivery.common.response.ApiResponse;
import com.fooddelivery.delivery.dto.DeliveryPartnerResponse;
import com.fooddelivery.delivery.dto.ManualAssignmentRequest;
import com.fooddelivery.delivery.service.DeliveryPartnerService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/admin/delivery")
@Validated
public class AdminDeliveryController {

    private final DeliveryPartnerService service;

    public AdminDeliveryController(DeliveryPartnerService service) {
        this.service = service;
    }

    @PostMapping("/assign")
    public ResponseEntity<ApiResponse<DeliveryPartnerResponse>> forceAssign(
            @RequestParam String orderId,
            @Valid @RequestBody ManualAssignmentRequest request) {
        DeliveryPartnerResponse response = service.forceAssign(request.getDriverId(), orderId);
        return ResponseEntity.ok(ApiResponse.success("Manual assignment completed successfully", response));
    }

    @PatchMapping("/{id}/suspend")
    public ResponseEntity<ApiResponse<DeliveryPartnerResponse>> suspendDriver(
            @PathVariable String id) {
        DeliveryPartnerResponse response = service.suspendDriver(id);
        return ResponseEntity.ok(ApiResponse.success("Driver suspended successfully", response));
    }

    @PatchMapping("/{id}/unsuspend")
    public ResponseEntity<ApiResponse<DeliveryPartnerResponse>> unsuspendDriver(
            @PathVariable String id) {
        DeliveryPartnerResponse response = service.unsuspendDriver(id);
        return ResponseEntity.ok(ApiResponse.success("Driver unsuspended successfully", response));
    }
}
