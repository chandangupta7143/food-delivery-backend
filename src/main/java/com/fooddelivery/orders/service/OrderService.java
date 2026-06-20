package com.fooddelivery.orders.service;

import com.fooddelivery.common.response.PaginatedResponse;
import com.fooddelivery.orders.dto.CreateOrderRequest;
import com.fooddelivery.orders.dto.FraudReviewRequest;
import com.fooddelivery.orders.dto.OrderResponse;
import com.fooddelivery.orders.entity.OrderStatus;

import java.util.List;

public interface OrderService {

    OrderResponse createOrder(CreateOrderRequest request, String email, String idempotencyKey);

    OrderResponse getOrderById(String id, String email);

    PaginatedResponse<OrderResponse> getMyOrders(String email, int page, int size);

    OrderResponse cancelOrder(String id, String email);

    OrderResponse updateOrderStatus(String id, OrderStatus status, String actorId, String actorType);

    OrderResponse reviewFraud(String id, FraudReviewRequest request, String adminEmail);

    OrderResponse forceCancelOrder(String id, String adminEmail);

    List<OrderResponse> getActiveVendorOrders(String restaurantId);
}
