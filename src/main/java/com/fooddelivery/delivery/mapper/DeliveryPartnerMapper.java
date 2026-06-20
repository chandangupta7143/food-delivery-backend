package com.fooddelivery.delivery.mapper;

import com.fooddelivery.delivery.dto.DeliveryPartnerResponse;
import com.fooddelivery.delivery.entity.DeliveryPartner;
import org.springframework.stereotype.Component;

@Component
public class DeliveryPartnerMapper {

    public DeliveryPartnerResponse toResponse(DeliveryPartner partner) {
        if (partner == null) {
            return null;
        }

        DeliveryPartnerResponse response = new DeliveryPartnerResponse();
        response.setId(partner.getId());
        response.setUserId(partner.getUserId());
        response.setVehicleType(partner.getVehicleType());

        if (partner.getCurrentLocation() != null) {
            response.setLongitude(partner.getCurrentLocation().getX());
            response.setLatitude(partner.getCurrentLocation().getY());
        }

        response.setLastLocationUpdateTime(partner.getLastLocationUpdateTime());
        response.setStatus(partner.getStatus());
        response.setCurrentOrderId(partner.getCurrentOrderId());
        response.setDailyDeliveryCount(partner.getDailyDeliveryCount());
        response.setAcceptanceRate(partner.getAcceptanceRate());
        response.setAverageDeliveryTimeMinutes(partner.getAverageDeliveryTimeMinutes());
        response.setRating(partner.getRating());
        response.setTotalAssignments(partner.getTotalAssignments());
        response.setTotalAccepted(partner.getTotalAccepted());
        response.setTotalRejected(partner.getTotalRejected());
        response.setTotalTimeouts(partner.getTotalTimeouts());

        return response;
    }
}
