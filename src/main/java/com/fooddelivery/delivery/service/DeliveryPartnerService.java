package com.fooddelivery.delivery.service;

import com.fooddelivery.delivery.dto.DeliveryPartnerResponse;
import com.fooddelivery.delivery.entity.DeliveryPartnerStatus;

public interface DeliveryPartnerService {

    DeliveryPartnerResponse updateAvailability(String email, DeliveryPartnerStatus status);

    DeliveryPartnerResponse updateLocation(String email, Double latitude, Double longitude);

    DeliveryPartnerResponse acceptAssignment(String email, String orderId);

    DeliveryPartnerResponse rejectAssignment(String email, String orderId);

    DeliveryPartnerResponse getAssignedOrder(String email);

    DeliveryPartnerResponse forceAssign(String driverId, String orderId);

    DeliveryPartnerResponse suspendDriver(String driverId);

    DeliveryPartnerResponse unsuspendDriver(String driverId);
}
