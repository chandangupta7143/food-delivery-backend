package com.fooddelivery.delivery.dto;

import jakarta.validation.constraints.NotBlank;

public class ManualAssignmentRequest {

    @NotBlank(message = "Driver ID is required")
    private String driverId;

    public ManualAssignmentRequest() {
    }

    public ManualAssignmentRequest(String driverId) {
        this.driverId = driverId;
    }

    public String getDriverId() {
        return driverId;
    }

    public void setDriverId(String driverId) {
        this.driverId = driverId;
    }
}
