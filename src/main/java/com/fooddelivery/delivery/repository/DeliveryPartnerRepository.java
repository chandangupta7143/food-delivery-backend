package com.fooddelivery.delivery.repository;

import com.fooddelivery.delivery.entity.DeliveryPartner;
import com.fooddelivery.delivery.entity.DeliveryPartnerStatus;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * MongoDB repository for DeliveryPartner documents.
 */
@Repository
public interface DeliveryPartnerRepository extends MongoRepository<DeliveryPartner, String> {

    Optional<DeliveryPartner> findByUserId(String userId);

    List<DeliveryPartner> findByStatus(DeliveryPartnerStatus status);

    List<DeliveryPartner> findByStatusAndCurrentOrderId(DeliveryPartnerStatus status, String orderId);
}
