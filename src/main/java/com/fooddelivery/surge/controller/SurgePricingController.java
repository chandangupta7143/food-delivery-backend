package com.fooddelivery.surge.controller;

import com.fooddelivery.common.response.ApiResponse;
import com.fooddelivery.surge.dto.*;
import com.fooddelivery.surge.service.SurgePricingService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.security.Principal;

@RestController
@RequestMapping("/api")
@Validated
public class SurgePricingController {

    private final SurgePricingService service;

    public SurgePricingController(SurgePricingService service) {
        this.service = service;
    }

    @PostMapping("/pricing/quote")
    public ResponseEntity<ApiResponse<PricingQuoteResponse>> getPricingQuote(
            @Valid @RequestBody PricingQuoteRequest request) {
        PricingQuoteResponse response = service.calculateQuote(request);
        return ResponseEntity.ok(ApiResponse.success("Pricing quote token generated successfully", response));
    }

    @PostMapping("/admin/surge/overrides")
    public ResponseEntity<ApiResponse<AdminSurgeOverrideResponse>> createOverride(
            @Valid @RequestBody AdminSurgeOverrideRequest request,
            Principal principal) {
        AdminSurgeOverrideResponse response = service.createOverride(request, principal.getName());
        return ResponseEntity.ok(ApiResponse.success("Surge pricing override created successfully", response));
    }

    @DeleteMapping("/admin/surge/overrides/{id}")
    public ResponseEntity<ApiResponse<Void>> removeOverride(
            @PathVariable String id) {
        service.removeOverride(id);
        return ResponseEntity.ok(ApiResponse.success("Surge pricing override removed successfully"));
    }

    @PostMapping("/admin/surge/emergency-disable")
    public ResponseEntity<ApiResponse<Void>> setEmergencyDisable(
            @RequestParam boolean disable,
            @RequestParam(required = false) String zoneName) {
        service.setEmergencyDisable(disable, zoneName);
        return ResponseEntity.ok(ApiResponse.success(
                disable ? "EMERGENCY PRICE SURGE DISABLE INITIATED SYSTEM-WIDE" : "Emergency price surge restrictions cleared"
        ));
    }
}
