package com.fooddelivery.delivery.dto;

import com.fooddelivery.delivery.entity.DeliveryPartnerStatus;
import jakarta.validation.constraints.NotNull;

public class AvailabilityUpdateRequest {

    @NotNull(message = "Status is required")
    private DeliveryPartnerStatus status;

    public AvailabilityUpdateRequest() {
    }

    public AvailabilityUpdateRequest(DeliveryPartnerStatus status) {
        this.status = status;
    }

    public DeliveryPartnerStatus getStatus() {
        return status;
    }

    public void setStatus(DeliveryPartnerStatus status) {
        this.status = status;
    }
}
