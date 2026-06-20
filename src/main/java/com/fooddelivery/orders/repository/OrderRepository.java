package com.fooddelivery.orders.repository;

import com.fooddelivery.orders.entity.Order;
import com.fooddelivery.orders.entity.OrderStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * MongoDB Repository for Order documents.
 */
@Repository
public interface OrderRepository extends MongoRepository<Order, String> {

    Optional<Order> findByOrderNumber(String orderNumber);

    Optional<Order> findByIdempotencyKey(String idempotencyKey);

    long countByUserIdAndFraudDetailsReviewStatus(String userId, com.fooddelivery.orders.entity.ReviewStatus reviewStatus);

    Page<Order> findByUserId(String userId, Pageable pageable);

    List<Order> findByUserIdAndCreatedAtAfter(String userId, java.time.LocalDateTime timestamp);

    List<Order> findByDeliveryAddressIgnoreCaseAndCreatedAtAfterAndPricingDiscountGreaterThan(
            String deliveryAddress, java.time.LocalDateTime timestamp, double minDiscount);

    List<Order> findByRestaurantIdAndStatusIn(String restaurantId, List<OrderStatus> statuses);

    List<Order> findByDeliveryPartnerIdAndStatusIn(String deliveryPartnerId, List<OrderStatus> statuses);

    List<Order> findByStatus(OrderStatus status);
}
