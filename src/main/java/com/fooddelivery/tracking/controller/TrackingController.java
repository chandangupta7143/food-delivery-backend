package com.fooddelivery.tracking.controller;

import com.fooddelivery.common.response.ApiResponse;
import com.fooddelivery.tracking.dto.TimelineEventResponse;
import com.fooddelivery.tracking.service.TrackingService;
import org.springframework.data.mongodb.core.geo.GeoJsonPoint;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.security.Principal;
import java.util.List;

@RestController
@RequestMapping("/api/tracking")
public class TrackingController {

    private final TrackingService service;

    public TrackingController(TrackingService service) {
        this.service = service;
    }

    @GetMapping("/orders/{orderId}/timeline")
    public ResponseEntity<ApiResponse<List<TimelineEventResponse>>> getTimeline(
            @PathVariable String orderId,
            Principal principal) {
        List<TimelineEventResponse> response = service.getVisibleTimeline(orderId, principal.getName());
        return ResponseEntity.ok(ApiResponse.success("Order timeline retrieved successfully", response));
    }

    @GetMapping("/orders/{orderId}/driver-location")
    public ResponseEntity<ApiResponse<DriverLocationResponse>> getDriverLocation(
            @PathVariable String orderId,
            Principal principal) {
        GeoJsonPoint loc = service.getFilteredDriverLocation(orderId, principal.getName());
        if (loc == null) {
            return ResponseEntity.ok(ApiResponse.success("Driver location is currently unavailable/obfuscated", null));
        }
        DriverLocationResponse response = new DriverLocationResponse(loc.getY(), loc.getX());
        return ResponseEntity.ok(ApiResponse.success("Driver location retrieved successfully", response));
    }

    public static class DriverLocationResponse {
        private Double latitude;
        private Double longitude;

        public DriverLocationResponse() {
        }

        public DriverLocationResponse(Double latitude, Double longitude) {
            this.latitude = latitude;
            this.longitude = longitude;
        }

        public Double getLatitude() {
            return latitude;
        }

        public void setLatitude(Double latitude) {
            this.latitude = latitude;
        }

        public Double getLongitude() {
            return longitude;
        }

        public void setLongitude(Double longitude) {
            this.longitude = longitude;
        }
    }
}
