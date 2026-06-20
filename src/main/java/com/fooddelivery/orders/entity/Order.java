package com.fooddelivery.orders.entity;

import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.annotation.Version;
import org.springframework.data.mongodb.core.geo.GeoJsonPoint;
import org.springframework.data.mongodb.core.index.CompoundIndex;
import org.springframework.data.mongodb.core.index.CompoundIndexes;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * Order domain entity stored in the 'orders' MongoDB collection.
 * Tracks client orders, payments, logistics, fraud decisions, and auditing histories.
 */
@Document(collection = "orders")
@CompoundIndexes({
        @CompoundIndex(name = "idx_user_created", def = "{'userId': 1, 'createdAt': -1}"),
        @CompoundIndex(name = "idx_restaurant_status_created", def = "{'restaurantId': 1, 'status': 1, 'createdAt': -1}"),
        @CompoundIndex(name = "idx_delivery_status", def = "{'deliveryPartnerId': 1, 'status': 1}"),
        @CompoundIndex(name = "idx_status_created", def = "{'status': 1, 'createdAt': 1}"),
        @CompoundIndex(name = "idx_fraud_status", def = "{'fraudDetails.reviewStatus': 1}"),
        @CompoundIndex(name = "idx_address_created", def = "{'deliveryAddress': 1, 'createdAt': -1}")
})
public class Order {

    @Id
    private String id;

    @Indexed(unique = true)
    private String orderNumber;

    @Indexed(unique = true, sparse = true)
    private String idempotencyKey;

    @Indexed
    private String userId;

    @Indexed
    private String restaurantId;
    private String restaurantName;
    private List<String> restaurantCuisines = new ArrayList<>();

    private List<OrderItem> items = new ArrayList<>();
    private OrderPricing pricing;

    private String deliveryAddress;
    private GeoJsonPoint deliveryCoordinates;
    private GeoJsonPoint restaurantCoordinates; // Snapshot at time of order

    @Indexed
    private String deliveryPartnerId;
    private LocalDateTime estimatedDeliveryTime;
    private int assignmentAttempts;

    @Indexed
    private OrderStatus status;
    private OrderSource orderSource;
    private PaymentStatus paymentStatus;
    private PaymentMethod paymentMethod;

    private FraudDetails fraudDetails;
    private LocalDateTime fraudCheckedAt;
    private String fraudDecisionBy;

    private List<StatusEvent> statusHistory = new ArrayList<>();

    @CreatedDate
    private LocalDateTime createdAt;

    @LastModifiedDate
    private LocalDateTime updatedAt;

    @Version
    private Long version;

    public Order() {
        this.status = OrderStatus.CREATED;
        this.paymentStatus = PaymentStatus.PENDING;
        this.fraudDetails = new FraudDetails();
        this.assignmentAttempts = 0;
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

    public String getIdempotencyKey() {
        return idempotencyKey;
    }

    public void setIdempotencyKey(String idempotencyKey) {
        this.idempotencyKey = idempotencyKey;
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

    public List<OrderItem> getItems() {
        return items;
    }

    public void setItems(List<OrderItem> items) {
        this.items = items;
    }

    public OrderPricing getPricing() {
        return pricing;
    }

    public void setPricing(OrderPricing pricing) {
        this.pricing = pricing;
    }

    public String getDeliveryAddress() {
        return deliveryAddress;
    }

    public void setDeliveryAddress(String deliveryAddress) {
        this.deliveryAddress = deliveryAddress;
    }

    public GeoJsonPoint getDeliveryCoordinates() {
        return deliveryCoordinates;
    }

    public void setDeliveryCoordinates(GeoJsonPoint deliveryCoordinates) {
        this.deliveryCoordinates = deliveryCoordinates;
    }

    public GeoJsonPoint getRestaurantCoordinates() {
        return restaurantCoordinates;
    }

    public void setRestaurantCoordinates(GeoJsonPoint restaurantCoordinates) {
        this.restaurantCoordinates = restaurantCoordinates;
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

    public FraudDetails getFraudDetails() {
        return fraudDetails;
    }

    public void setFraudDetails(FraudDetails fraudDetails) {
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

    public List<StatusEvent> getStatusHistory() {
        return statusHistory;
    }

    public void setStatusHistory(List<StatusEvent> statusHistory) {
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

    public Long getVersion() {
        return version;
    }

    public void setVersion(Long version) {
        this.version = version;
    }
}
