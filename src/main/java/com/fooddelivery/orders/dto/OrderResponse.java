package com.fooddelivery.orders.dto;

import com.fooddelivery.orders.entity.OrderSource;
import com.fooddelivery.orders.entity.OrderStatus;
import com.fooddelivery.orders.entity.PaymentMethod;
import com.fooddelivery.orders.entity.PaymentStatus;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

public class OrderResponse {

    private String id;
    private String orderNumber;
    private String userId;
    private String restaurantId;
    private String restaurantName;
    private List<String> restaurantCuisines = new ArrayList<>();

    private List<OrderItemResponse> items = new ArrayList<>();
    private OrderPricingResponse pricing;

    private String deliveryAddress;
    private Double deliveryLatitude;
    private Double deliveryLongitude;
    private Double restaurantLatitude;
    private Double restaurantLongitude;

    private String deliveryPartnerId;
    private LocalDateTime estimatedDeliveryTime;
    private int assignmentAttempts;

    private OrderStatus status;
    private OrderSource orderSource;
    private PaymentStatus paymentStatus;
    private PaymentMethod paymentMethod;

    private FraudDetailsResponse fraudDetails;
    private LocalDateTime fraudCheckedAt;
    private String fraudDecisionBy;

    private List<StatusEventResponse> statusHistory = new ArrayList<>();

    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    public OrderResponse() {
    }

    // Getters and Setters

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getOrderNumber() {
        return orderNumber;
    }

    public void setOrderNumber(String orderNumber) {
        this.orderNumber = orderNumber;
    }

    public String getUserId() {
        return userId;
    }

    public void setUserId(String userId) {
        this.userId = userId;
    }

    public String getRestaurantId() {
        return restaurantId;
    }

    public void setRestaurantId(String restaurantId) {
        this.restaurantId = restaurantId;
    }

    public String getRestaurantName() {
        return restaurantName;
    }

    public void setRestaurantName(String restaurantName) {
        this.restaurantName = restaurantName;
    }

    public List<String> getRestaurantCuisines() {
        return restaurantCuisines;
    }

    public void setRestaurantCuisines(List<String> restaurantCuisines) {
        this.restaurantCuisines = restaurantCuisines;
    }

    public List<OrderItemResponse> getItems() {
        return items;
    }

    public void setItems(List<OrderItemResponse> items) {
        this.items = items;
    }

    public OrderPricingResponse getPricing() {
        return pricing;
    }

    public void setPricing(OrderPricingResponse pricing) {
        this.pricing = pricing;
    }

    public String getDeliveryAddress() {
        return deliveryAddress;
    }

    public void setDeliveryAddress(String deliveryAddress) {
        this.deliveryAddress = deliveryAddress;
    }

    public Double getDeliveryLatitude() {
        return deliveryLatitude;
    }

    public void setDeliveryLatitude(Double deliveryLatitude) {
        this.deliveryLatitude = deliveryLatitude;
    }

    public Double getDeliveryLongitude() {
        return deliveryLongitude;
    }

    public void setDeliveryLongitude(Double deliveryLongitude) {
        this.deliveryLongitude = deliveryLongitude;
    }

    public Double getRestaurantLatitude() {
        return restaurantLatitude;
    }

    public void setRestaurantLatitude(Double restaurantLatitude) {
        this.restaurantLatitude = restaurantLatitude;
    }

    public Double getRestaurantLongitude() {
        return restaurantLongitude;
    }

    public void setRestaurantLongitude(Double restaurantLongitude) {
        this.restaurantLongitude = restaurantLongitude;
    }

    public String getDeliveryPartnerId() {
        return deliveryPartnerId;
    }

    public void setDeliveryPartnerId(String deliveryPartnerId) {
        this.deliveryPartnerId = deliveryPartnerId;
    }

    public LocalDateTime getEstimatedDeliveryTime() {
        return estimatedDeliveryTime;
    }

    public void setEstimatedDeliveryTime(LocalDateTime estimatedDeliveryTime) {
        this.estimatedDeliveryTime = estimatedDeliveryTime;
    }

    public int getAssignmentAttempts() {
        return assignmentAttempts;
    }

    public void setAssignmentAttempts(int assignmentAttempts) {
        this.assignmentAttempts = assignmentAttempts;
    }

    public OrderStatus getStatus() {
        return status;
    }

    public void setStatus(OrderStatus status) {
        this.status = status;
    }

    public OrderSource getOrderSource() {
        return orderSource;
    }

    public void setOrderSource(OrderSource orderSource) {
        this.orderSource = orderSource;
    }

    public PaymentStatus getPaymentStatus() {
        return paymentStatus;
    }

    public void setPaymentStatus(PaymentStatus paymentStatus) {
        this.paymentStatus = paymentStatus;
    }

    public PaymentMethod getPaymentMethod() {
        return paymentMethod;
    }

    public void setPaymentMethod(PaymentMethod paymentMethod) {
        this.paymentMethod = paymentMethod;
    }

    public FraudDetailsResponse getFraudDetails() {
        return fraudDetails;
    }

    public void setFraudDetails(FraudDetailsResponse fraudDetails) {
        this.fraudDetails = fraudDetails;
    }

    public LocalDateTime getFraudCheckedAt() {
        return fraudCheckedAt;
    }

    public void setFraudCheckedAt(LocalDateTime fraudCheckedAt) {
        this.fraudCheckedAt = fraudCheckedAt;
    }

    public String getFraudDecisionBy() {
        return fraudDecisionBy;
    }

    public void setFraudDecisionBy(String fraudDecisionBy) {
        this.fraudDecisionBy = fraudDecisionBy;
    }

    public List<StatusEventResponse> getStatusHistory() {
        return statusHistory;
    }

    public void setStatusHistory(List<StatusEventResponse> statusHistory) {
        this.statusHistory = statusHistory;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(LocalDateTime updatedAt) {
        this.updatedAt = updatedAt;
    }
}
