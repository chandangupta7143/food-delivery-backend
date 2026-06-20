package com.fooddelivery.delivery.controller;

import com.fooddelivery.common.response.ApiResponse;
import com.fooddelivery.delivery.dto.AvailabilityUpdateRequest;
import com.fooddelivery.delivery.dto.DeliveryPartnerResponse;
import com.fooddelivery.delivery.dto.LocationUpdateRequest;
import com.fooddelivery.delivery.service.DeliveryPartnerService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.security.Principal;

@RestController
@RequestMapping("/api/delivery")
@Validated
public class DeliveryPartnerController {

    private final DeliveryPartnerService service;

    public DeliveryPartnerController(DeliveryPartnerService service) {
        this.service = service;
    }

    @PatchMapping("/availability")
    public ResponseEntity<ApiResponse<DeliveryPartnerResponse>> updateAvailability(
            @Valid @RequestBody AvailabilityUpdateRequest request,
            Principal principal) {
        DeliveryPartnerResponse response = service.updateAvailability(principal.getName(), request.getStatus());
        return ResponseEntity.ok(ApiResponse.success("Availability updated successfully", response));
    }

    @PostMapping("/location")
    public ResponseEntity<ApiResponse<DeliveryPartnerResponse>> updateLocation(
            @Valid @RequestBody LocationUpdateRequest request,
            Principal principal) {
        DeliveryPartnerResponse response = service.updateLocation(principal.getName(), request.getLatitude(), request.getLongitude());
        return ResponseEntity.ok(ApiResponse.success("Location updated successfully", response));
    }

    @GetMapping("/assigned-order")
    public ResponseEntity<ApiResponse<DeliveryPartnerResponse>> getAssignedOrder(
            Principal principal) {
        DeliveryPartnerResponse response = service.getAssignedOrder(principal.getName());
        return ResponseEntity.ok(ApiResponse.success("Assigned order details retrieved", response));
    }

    @PostMapping("/orders/{orderId}/accept")
    public ResponseEntity<ApiResponse<DeliveryPartnerResponse>> acceptAssignment(
            @PathVariable String orderId,
            Principal principal) {
        DeliveryPartnerResponse response = service.acceptAssignment(principal.getName(), orderId);
        return ResponseEntity.ok(ApiResponse.success("Assignment accepted successfully", response));
    }

    @PostMapping("/orders/{orderId}/reject")
    public ResponseEntity<ApiResponse<DeliveryPartnerResponse>> rejectAssignment(
            @PathVariable String orderId,
            Principal principal) {
        DeliveryPartnerResponse response = service.rejectAssignment(principal.getName(), orderId);
        return ResponseEntity.ok(ApiResponse.success("Assignment rejected successfully", response));
    }
}
