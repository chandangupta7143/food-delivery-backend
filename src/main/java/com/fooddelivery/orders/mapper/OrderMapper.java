package com.fooddelivery.orders.mapper;

import com.fooddelivery.orders.dto.*;
import com.fooddelivery.orders.entity.*;
import org.springframework.stereotype.Component;

import java.util.stream.Collectors;

@Component
public class OrderMapper {

    public OrderResponse toResponse(Order order) {
        if (order == null) {
            return null;
        }

        OrderResponse response = new OrderResponse();
        response.setId(order.getId());
        response.setOrderNumber(order.getOrderNumber());
        response.setUserId(order.getUserId());
        response.setRestaurantId(order.getRestaurantId());
        response.setRestaurantName(order.getRestaurantName());
        response.setRestaurantCuisines(order.getRestaurantCuisines());

        if (order.getItems() != null) {
            response.setItems(order.getItems().stream()
                    .map(this::toOrderItemResponse)
                    .collect(Collectors.toList()));
        }

        response.setPricing(toOrderPricingResponse(order.getPricing()));
        response.setDeliveryAddress(order.getDeliveryAddress());

        if (order.getDeliveryCoordinates() != null) {
            response.setDeliveryLongitude(order.getDeliveryCoordinates().getX());
            response.setDeliveryLatitude(order.getDeliveryCoordinates().getY());
        }

        if (order.getRestaurantCoordinates() != null) {
            response.setRestaurantLongitude(order.getRestaurantCoordinates().getX());
            response.setRestaurantLatitude(order.getRestaurantCoordinates().getY());
        }

        response.setDeliveryPartnerId(order.getDeliveryPartnerId());
        response.setEstimatedDeliveryTime(order.getEstimatedDeliveryTime());
        response.setAssignmentAttempts(order.getAssignmentAttempts());

        response.setStatus(order.getStatus());
        response.setOrderSource(order.getOrderSource());
        response.setPaymentStatus(order.getPaymentStatus());
        response.setPaymentMethod(order.getPaymentMethod());

        response.setFraudDetails(toFraudDetailsResponse(order.getFraudDetails()));
        response.setFraudCheckedAt(order.getFraudCheckedAt());
        response.setFraudDecisionBy(order.getFraudDecisionBy());

        if (order.getStatusHistory() != null) {
            response.setStatusHistory(order.getStatusHistory().stream()
                    .map(this::toStatusEventResponse)
                    .collect(Collectors.toList()));
        }

        response.setCreatedAt(order.getCreatedAt());
        response.setUpdatedAt(order.getUpdatedAt());

        return response;
    }

    public OrderItemResponse toOrderItemResponse(OrderItem item) {
        if (item == null) {
            return null;
        }
        return new OrderItemResponse(
                item.getItemId(),
                item.getName(),
                item.getQuantity(),
                item.getUnitPrice(),
                item.getTotalPrice()
        );
    }

    public OrderPricingResponse toOrderPricingResponse(OrderPricing pricing) {
        if (pricing == null) {
            return null;
        }
        return new OrderPricingResponse(
                pricing.getItemTotal(),
                pricing.getDiscount(),
                pricing.getSubtotal(),
                pricing.getTaxes(),
                pricing.getPlatformFee(),
                pricing.getDeliveryFee(),
                pricing.getSurgeFee(),
                pricing.getFinalPayable()
        );
    }

    public FraudDetailsResponse toFraudDetailsResponse(FraudDetails fraud) {
        if (fraud == null) {
            return null;
        }
        return new FraudDetailsResponse(
                fraud.getRiskScore(),
                fraud.getFraudFlags(),
                fraud.getReviewStatus(),
                fraud.getAdminReviewNotes()
        );
    }

    public StatusEventResponse toStatusEventResponse(StatusEvent event) {
        if (event == null) {
            return null;
        }
        return new StatusEventResponse(
                event.getStatus(),
                event.getEventType(),
                event.getTimestamp(),
                event.getActorId(),
                event.getActorType()
        );
    }
}
