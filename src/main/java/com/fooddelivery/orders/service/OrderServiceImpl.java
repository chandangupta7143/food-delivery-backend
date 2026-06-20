package com.fooddelivery.orders.service;

import com.fooddelivery.common.exception.ResourceNotFoundException;
import com.fooddelivery.common.response.PaginatedResponse;
import com.fooddelivery.orders.dto.CreateOrderItemRequest;
import com.fooddelivery.orders.dto.CreateOrderRequest;
import com.fooddelivery.orders.dto.FraudReviewRequest;
import com.fooddelivery.orders.dto.OrderResponse;
import com.fooddelivery.orders.entity.*;
import com.fooddelivery.orders.mapper.OrderMapper;
import com.fooddelivery.orders.repository.OrderRepository;
import com.fooddelivery.restaurants.entity.Restaurant;
import com.fooddelivery.restaurants.repository.RestaurantRepository;
import com.fooddelivery.users.entity.User;
import com.fooddelivery.users.repository.UserRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.mongodb.core.geo.GeoJsonPoint;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import com.fooddelivery.common.enums.Role;
import org.springframework.dao.DuplicateKeyException;
import com.fooddelivery.delivery.service.AssignmentEngine;
import com.fooddelivery.notifications.service.NotificationService;
import com.fooddelivery.notifications.entity.NotificationType;
import com.fooddelivery.fraud.service.FraudEvaluationService;
import com.fooddelivery.fraud.repository.UserRestrictionRepository;
import com.fooddelivery.fraud.entity.UserRestriction;
import com.fooddelivery.surge.service.HmacTokenService;
import com.uber.h3core.H3Core;

import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class OrderServiceImpl implements OrderService {

    private final OrderRepository orderRepository;
    private final UserRepository userRepository;
    private final RestaurantRepository restaurantRepository;
    private final OrderMapper orderMapper;
    private final com.fooddelivery.delivery.service.AssignmentEngine assignmentEngine;
    private final NotificationService notificationService;
    private final FraudEvaluationService fraudEvaluationService;
    private final UserRestrictionRepository userRestrictionRepository;
    private final HmacTokenService hmacTokenService;
    private final H3Core h3;
    private final com.fooddelivery.recommendations.service.RecommendationService recommendationService;

    public OrderServiceImpl(OrderRepository orderRepository,
                            UserRepository userRepository,
                            RestaurantRepository restaurantRepository,
                            OrderMapper orderMapper,
                            com.fooddelivery.delivery.service.AssignmentEngine assignmentEngine,
                            NotificationService notificationService,
                            FraudEvaluationService fraudEvaluationService,
                            UserRestrictionRepository userRestrictionRepository,
                            HmacTokenService hmacTokenService,
                            H3Core h3,
                            com.fooddelivery.recommendations.service.RecommendationService recommendationService) {
        this.orderRepository = orderRepository;
        this.userRepository = userRepository;
        this.restaurantRepository = restaurantRepository;
        this.orderMapper = orderMapper;
        this.assignmentEngine = assignmentEngine;
        this.notificationService = notificationService;
        this.fraudEvaluationService = fraudEvaluationService;
        this.userRestrictionRepository = userRestrictionRepository;
        this.hmacTokenService = hmacTokenService;
        this.h3 = h3;
        this.recommendationService = recommendationService;
    }

    @Override
    @Transactional
    public OrderResponse createOrder(CreateOrderRequest request, String email, String idempotencyKey) {
        // 1. Check Idempotency Key
        if (idempotencyKey != null && !idempotencyKey.trim().isEmpty()) {
            Optional<Order> existingOrder = orderRepository.findByIdempotencyKey(idempotencyKey);
            if (existingOrder.isPresent()) {
                return orderMapper.toResponse(existingOrder.get());
            }
        }

        // 2. Lookup User
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new ResourceNotFoundException("User not found with email: " + email));

        // 3. Lookup & Verify Restaurant
        Restaurant restaurant = restaurantRepository.findById(request.getRestaurantId())
                .orElseThrow(() -> new ResourceNotFoundException("Restaurant not found with id: " + request.getRestaurantId()));

        if (!restaurant.getIsActive() || !restaurant.getIsVerified() || restaurant.getIsDeleted()) {
            throw new IllegalArgumentException("Restaurant is inactive, unverified, or deleted");
        }

        // 4. Map and Calculate Pricing (Immutability Enforced on Creation)
        List<OrderItem> items = new ArrayList<>();
        double itemTotal = 0.0;

        for (CreateOrderItemRequest itemReq : request.getItems()) {
            MockMenuItem mockItem = getMockMenuItem(itemReq.getItemId());
            double totalPrice = mockItem.price * itemReq.getQuantity();
            itemTotal += totalPrice;

            items.add(new OrderItem(
                    itemReq.getItemId(),
                    mockItem.name,
                    itemReq.getQuantity(),
                    mockItem.price,
                    totalPrice
            ));
        }

        boolean couponBlocked = false;
        Optional<UserRestriction> userRest = userRestrictionRepository.findByUserId(user.getId());
        if (userRest.isPresent()) {
            UserRestriction restriction = userRest.get();
            if (restriction.getExpiresAt().isAfter(LocalDateTime.now()) &&
                    restriction.getRestrictionTypes().contains("BLOCK_COUPONS")) {
                couponBlocked = true;
            }
        }

        double discount = (!couponBlocked && itemTotal > 500.0) ? 50.0 : 0.0;
        double subtotal = itemTotal - discount;
        double taxes = subtotal * 0.05; // 5% tax
        double platformFee = 15.0;

        // Resolve H3 cell Resolution 8 index of the delivery coordinates
        String h3Index;
        try {
            h3Index = h3.latLngToCellAddress(request.getDeliveryLatitude(), request.getDeliveryLongitude(), 8);
        } catch (Exception e) {
            throw new IllegalArgumentException("Invalid coordinate parameters provided");
        }

        double surgeMultiplier = request.getSurgeMultiplier() != null ? request.getSurgeMultiplier() : 1.0;
        double baseFee = 30.0;
        double quotedDeliveryFee = Math.round((baseFee * surgeMultiplier) * 100.0) / 100.0;

        boolean tokenValid = false;
        if (request.getQuoteToken() != null && !request.getQuoteToken().isEmpty()) {
            String tokenUserId = "";
            try {
                String[] parts = request.getQuoteToken().split("\\.");
                if (parts.length == 2) {
                    String decodedPayload = new String(Base64.getUrlDecoder().decode(parts[0]), java.nio.charset.StandardCharsets.UTF_8);
                    String[] fields = decodedPayload.split(":");
                    if (fields.length >= 6 && fields.length <= 7) {
                        tokenUserId = fields[0];
                    }
                }
            } catch (Exception e) {
                // Ignore
            }

            if (tokenUserId.equals(user.getId()) || tokenUserId.equals(user.getEmail())) {
                tokenValid = hmacTokenService.verifyToken(
                        request.getQuoteToken(),
                        tokenUserId,
                        request.getRestaurantId(),
                        h3Index,
                        surgeMultiplier,
                        quotedDeliveryFee
                );
            }
        }

        if (!tokenValid) {
            throw new SecurityException("Invalid, expired, or tampered pricing quote token");
        }

        double deliveryFee = baseFee;
        double surgeFee = Math.round((baseFee * (surgeMultiplier - 1.0)) * 100.0) / 100.0;
        double finalPayable = subtotal + taxes + platformFee + deliveryFee + surgeFee;

        OrderPricing pricing = new OrderPricing(
                itemTotal,
                discount,
                subtotal,
                taxes,
                platformFee,
                deliveryFee,
                surgeFee,
                finalPayable
        );

        // 5. Build Order entity
        Order order = new Order();
        order.setOrderNumber(generateOrderNumber());
        order.setIdempotencyKey(idempotencyKey);
        order.setUserId(user.getId());
        order.setRestaurantId(restaurant.getId());
        order.setRestaurantName(restaurant.getName());
        // Snapshot cuisines
        order.setRestaurantCuisines(new ArrayList<>(restaurant.getCuisines()));

        order.setItems(items);
        order.setPricing(pricing);

        order.setDeliveryAddress(request.getDeliveryAddress());
        order.setDeliveryCoordinates(new GeoJsonPoint(request.getDeliveryLongitude(), request.getDeliveryLatitude()));
        if (restaurant.getLocation() != null) {
            order.setRestaurantCoordinates(new GeoJsonPoint(restaurant.getLocation().getX(), restaurant.getLocation().getY()));
        }

        order.setOrderSource(request.getOrderSource());
        order.setPaymentMethod(request.getPaymentMethod());
        order.setPaymentStatus(PaymentStatus.PENDING);

        // 6. Fraud Check Integration (Locked Flow)
        fraudEvaluationService.evaluateOrder(order);

        // Add status history entry based on what the fraud engine decided
        if (order.getStatus() == OrderStatus.REJECTED) {
            addStatusHistory(order, OrderStatus.REJECTED, EventType.SYSTEM_EVENT, "SYSTEM", "SYSTEM");
        } else if (order.getStatus() == OrderStatus.PENDING_REVIEW) {
            addStatusHistory(order, OrderStatus.PENDING_REVIEW, EventType.SYSTEM_EVENT, "SYSTEM", "SYSTEM");
        } else {
            addStatusHistory(order, OrderStatus.CREATED, EventType.SYSTEM_EVENT, "SYSTEM", "SYSTEM");
        }

        try {
            order = orderRepository.save(order);

            // Log conversion event for CTR/CVR attribution
            if (request.getRecommendationEventId() != null && !request.getRecommendationEventId().isEmpty()) {
                try {
                    recommendationService.attributeOrderConversion(
                            request.getRecommendationEventId(),
                            order.getId(),
                            order.getRestaurantId(),
                            order.getPricing().getFinalPayable()
                    );
                } catch (Exception ex) {
                    // Fail silently to not impact checkout flows
                }
            }

            // Update user recommendation affinities based on the ordered cuisine
            try {
                String primaryCuisine = (restaurant.getCuisines() != null && !restaurant.getCuisines().isEmpty())
                        ? restaurant.getCuisines().get(0) : "Other";
                recommendationService.updateUserAffinities(
                        user.getId(),
                        order.getRestaurantId(),
                        primaryCuisine,
                        order.getPricing().getFinalPayable()
                );
            } catch (Exception ex) {
                // Fail silently to not impact checkout flows
            }

            sendOrderStatusNotification(order);
            return orderMapper.toResponse(order);
        } catch (DuplicateKeyException e) {
            if (idempotencyKey != null && !idempotencyKey.trim().isEmpty()) {
                return orderRepository.findByIdempotencyKey(idempotencyKey)
                        .map(orderMapper::toResponse)
                        .orElseThrow(() -> e);
            }
            throw e;
        }
    }

    @Override
    public OrderResponse getOrderById(String id, String email) {
        Order order = findOrderOrThrow(id);
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new ResourceNotFoundException("User not found with email: " + email));

        // Security check: Only the order owner or ADMIN can view the order
        if (!order.getUserId().equals(user.getId()) && !user.getRole().name().equals("ADMIN")) {
            throw new SecurityException("You do not have permission to view this order");
        }

        return orderMapper.toResponse(order);
    }

    @Override
    public PaginatedResponse<OrderResponse> getMyOrders(String email, int page, int size) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new ResourceNotFoundException("User not found with email: " + email));

        Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "createdAt"));
        Page<Order> result = orderRepository.findByUserId(user.getId(), pageable);

        List<OrderResponse> content = result.getContent().stream()
                .map(orderMapper::toResponse)
                .collect(Collectors.toList());

        return new PaginatedResponse<>(content, result.getNumber(), result.getSize(), result.getTotalElements(), result.getTotalPages());
    }

    @Override
    @Transactional
    public OrderResponse cancelOrder(String id, String email) {
        Order order = findOrderOrThrow(id);
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new ResourceNotFoundException("User not found with email: " + email));

        // Security check
        if (!order.getUserId().equals(user.getId()) && !user.getRole().name().equals("ADMIN")) {
            throw new SecurityException("You do not have permission to cancel this order");
        }

        // Check active BLOCK_REFUNDS restriction
        if (user.getRole() == Role.USER) {
            Optional<UserRestriction> userRest = userRestrictionRepository.findByUserId(user.getId());
            if (userRest.isPresent()) {
                UserRestriction restriction = userRest.get();
                if (restriction.getExpiresAt().isAfter(LocalDateTime.now()) &&
                        restriction.getRestrictionTypes().contains("BLOCK_REFUNDS")) {
                    throw new SecurityException("Your account is restricted from initiating refunds or cancellations.");
                }
            }
        }

        // Guard: User cancellations only allowed in CREATED or PENDING_REVIEW
        if (order.getStatus() != OrderStatus.CREATED && order.getStatus() != OrderStatus.PENDING_REVIEW) {
            throw new IllegalStateException("Order cannot be cancelled in its current state: " + order.getStatus());
        }

        addStatusHistory(order, OrderStatus.CANCELLED, EventType.USER_ACTION, user.getId(), "USER");
        order.setPaymentStatus(PaymentStatus.REFUNDED); // If prepaid

        order = orderRepository.save(order);
        sendOrderStatusNotification(order);
        return orderMapper.toResponse(order);
    }

    @Override
    @Transactional
    public OrderResponse updateOrderStatus(String id, OrderStatus status, String actorId, String actorType) {
        Order order = findOrderOrThrow(id);
        User actor = userRepository.findByEmail(actorId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found with email: " + actorId));

        // Enforce role-based restrictions
        if (actor.getRole() == Role.USER) {
            throw new SecurityException("Customers are not authorized to update order status directly");
        }

        if (status == OrderStatus.OUT_FOR_DELIVERY || status == OrderStatus.DELIVERED) {
            if (actor.getRole() == Role.DELIVERY_PARTNER) {
                if (order.getDeliveryPartnerId() != null && !order.getDeliveryPartnerId().equals(actor.getId())) {
                    throw new SecurityException("You are not the assigned delivery partner for this order");
                }
            } else if (actor.getRole() != Role.ADMIN) {
                throw new SecurityException("Unauthorized delivery partner transition");
            }
        }

        if (status == OrderStatus.RESTAURANT_ACCEPTED || status == OrderStatus.PREPARING || status == OrderStatus.READY_FOR_PICKUP) {
            if (actor.getRole() != Role.ADMIN) {
                throw new SecurityException("Merchant status transitions require administrative/merchant privileges");
            }
        }

        OrderStatus currentStatus = order.getStatus();

        // Locked Order Status Machine Transition Guards
        validateTransition(currentStatus, status);

        // Track delivery assignment attempts
        if (status == OrderStatus.READY_FOR_PICKUP) {
            order.setStatus(status);
            addStatusHistory(order, status, mapActorTypeToEventType(actorType), actor.getId(), actor.getRole().name());
            order = orderRepository.save(order);
            
            // Trigger actual smart geo assignment engine
            assignmentEngine.assignDriver(order);
            
            // Reload the assigned order state
            order = findOrderOrThrow(order.getId());
        } else {
            addStatusHistory(order, status, mapActorTypeToEventType(actorType), actor.getId(), actor.getRole().name());
        }

        if (status == OrderStatus.DELIVERED) {
            order.setPaymentStatus(PaymentStatus.PAID);
        }

        order = orderRepository.save(order);
        sendOrderStatusNotification(order);
        return orderMapper.toResponse(order);
    }

    @Override
    @Transactional
    public OrderResponse reviewFraud(String id, FraudReviewRequest request, String adminEmail) {
        Order order = findOrderOrThrow(id);
        User admin = userRepository.findByEmail(adminEmail)
                .orElseThrow(() -> new ResourceNotFoundException("Admin user not found"));

        if (!admin.getRole().name().equals("ADMIN")) {
            throw new SecurityException("Only admin users can perform fraud reviews");
        }

        if (order.getStatus() != OrderStatus.PENDING_REVIEW) {
            throw new IllegalStateException("Order is not pending fraud review");
        }

        order.getFraudDetails().setReviewStatus(request.getDecision());
        order.getFraudDetails().setAdminReviewNotes(request.getAdminNotes());
        order.setFraudDecisionBy(admin.getName() + " (" + admin.getId() + ")");
        order.setFraudCheckedAt(LocalDateTime.now());

        if (request.getDecision() == ReviewStatus.PASSED) {
            addStatusHistory(order, OrderStatus.CREATED, EventType.ADMIN_OVERRIDE, admin.getId(), "ADMIN");
        } else if (request.getDecision() == ReviewStatus.REJECTED) {
            addStatusHistory(order, OrderStatus.REJECTED, EventType.ADMIN_OVERRIDE, admin.getId(), "ADMIN");
            order.setPaymentStatus(PaymentStatus.REFUNDED);
        }

        order = orderRepository.save(order);
        sendOrderStatusNotification(order);
        return orderMapper.toResponse(order);
    }

    @Override
    @Transactional
    public OrderResponse forceCancelOrder(String id, String adminEmail) {
        Order order = findOrderOrThrow(id);
        User admin = userRepository.findByEmail(adminEmail)
                .orElseThrow(() -> new ResourceNotFoundException("Admin user not found"));

        if (!admin.getRole().name().equals("ADMIN")) {
            throw new SecurityException("Only admin users can force cancel orders");
        }

        addStatusHistory(order, OrderStatus.CANCELLED, EventType.ADMIN_OVERRIDE, admin.getId(), "ADMIN");
        if (order.getPaymentStatus() == PaymentStatus.PAID) {
            order.setPaymentStatus(PaymentStatus.REFUNDED);
        }

        order = orderRepository.save(order);
        sendOrderStatusNotification(order);
        return orderMapper.toResponse(order);
    }

    @Override
    public List<OrderResponse> getActiveVendorOrders(String restaurantId) {
        List<OrderStatus> activeStatuses = Arrays.asList(
                OrderStatus.CREATED,
                OrderStatus.RESTAURANT_ACCEPTED,
                OrderStatus.PREPARING,
                OrderStatus.READY_FOR_PICKUP
        );
        List<Order> orders = orderRepository.findByRestaurantIdAndStatusIn(restaurantId, activeStatuses);
        return orders.stream()
                .map(orderMapper::toResponse)
                .collect(Collectors.toList());
    }

    private Order findOrderOrThrow(String id) {
        return orderRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Order not found with id: " + id));
    }

    private void addStatusHistory(Order order, OrderStatus status, EventType eventType, String actorId, String actorType) {
        order.setStatus(status);
        StatusEvent event = new StatusEvent(status, eventType, LocalDateTime.now(), actorId, actorType);
        order.getStatusHistory().add(event);
    }

    private String generateOrderNumber() {
        return "ORD-" + (System.currentTimeMillis() % 10000000) + String.format("%02d", new Random().nextInt(100));
    }

    private EventType mapActorTypeToEventType(String actorType) {
        if (actorType == null) return EventType.SYSTEM_EVENT;
        switch (actorType.toUpperCase()) {
            case "USER": return EventType.USER_ACTION;
            case "RESTAURANT":
            case "VENDOR": return EventType.VENDOR_ACTION;
            case "ADMIN": return EventType.ADMIN_OVERRIDE;
            case "DELIVERY_PARTNER":
            case "DRIVER": return EventType.DELIVERY_PARTNER_ACTION;
            default: return EventType.SYSTEM_EVENT;
        }
    }

    private void validateTransition(OrderStatus current, OrderStatus next) {
        if (current == next) {
            return;
        }

        // Terminal states cannot transition to anything
        if (current == OrderStatus.DELIVERED || current == OrderStatus.CANCELLED || current == OrderStatus.REJECTED) {
            throw new IllegalStateException("Cannot transition from terminal state: " + current);
        }

        boolean valid = false;
        switch (current) {
            case CREATED:
                valid = (next == OrderStatus.PENDING_REVIEW || next == OrderStatus.RESTAURANT_ACCEPTED ||
                        next == OrderStatus.REJECTED || next == OrderStatus.CANCELLED);
                break;
            case PENDING_REVIEW:
                valid = (next == OrderStatus.CREATED ||
                        next == OrderStatus.REJECTED || next == OrderStatus.CANCELLED);
                break;
            case RESTAURANT_ACCEPTED:
                valid = (next == OrderStatus.PREPARING || next == OrderStatus.CANCELLED);
                break;
            case PREPARING:
                valid = (next == OrderStatus.READY_FOR_PICKUP || next == OrderStatus.CANCELLED);
                break;
            case READY_FOR_PICKUP:
                valid = (next == OrderStatus.OUT_FOR_DELIVERY || next == OrderStatus.CANCELLED);
                break;
            case OUT_FOR_DELIVERY:
                valid = (next == OrderStatus.DELIVERED || next == OrderStatus.CANCELLED);
                break;
        }

        if (!valid) {
            throw new IllegalStateException("Invalid status transition from " + current + " to " + next);
        }
    }

    private MockMenuItem getMockMenuItem(String itemId) {
        switch (itemId) {
            case "item_1": return new MockMenuItem("Chicken Biryani", 250.0);
            case "item_2": return new MockMenuItem("Paneer Butter Masala", 180.0);
            case "item_3": return new MockMenuItem("Garlic Naan", 40.0);
            case "item_4": return new MockMenuItem("Chocolate Brownie", 120.0);
            case "item_5": return new MockMenuItem("Mango Shake", 90.0);
            default: return new MockMenuItem("Special Dish (" + itemId + ")", 150.0);
        }
    }

    private static class MockMenuItem {
        final String name;
        final double price;

        MockMenuItem(String name, double price) {
            this.name = name;
            this.price = price;
        }
    }

    private void sendOrderStatusNotification(Order order) {
        String title;
        String body;
        String priority = "MEDIUM";

        switch (order.getStatus()) {
            case CREATED:
                title = "Order Placed Successfully";
                body = "Your order from " + order.getRestaurantName() + " has been placed! Order Number: " + order.getOrderNumber();
                break;
            case PENDING_REVIEW:
                title = "Order Under Verification";
                body = "Your order is undergoing a verification check.";
                priority = "HIGH";
                break;
            case RESTAURANT_ACCEPTED:
                title = "Order Accepted";
                body = "Great news! " + order.getRestaurantName() + " has accepted your order.";
                break;
            case PREPARING:
                title = "Preparing Your Meal";
                body = "The chef is preparing your fresh meal now!";
                break;
            case READY_FOR_PICKUP:
                title = "Order Ready for Pickup";
                body = "Your order is ready! A delivery partner is assigned.";
                priority = "HIGH";
                break;
            case OUT_FOR_DELIVERY:
                title = "Order Out for Delivery";
                body = "Your food is on the way! Live tracking is active.";
                priority = "HIGH";
                break;
            case DELIVERED:
                title = "Order Delivered";
                body = "Delicious food delivered! Enjoy your meal.";
                priority = "HIGH";
                break;
            case CANCELLED:
                title = "Order Cancelled";
                body = "Your order has been cancelled and a refund is initiated.";
                priority = "HIGH";
                break;
            case REJECTED:
                title = "Order Rejected";
                body = "We are sorry, your order could not be accepted.";
                priority = "HIGH";
                break;
            default:
                return;
        }

        try {
            notificationService.sendNotification(
                    order.getUserId(),
                    java.util.UUID.randomUUID().toString(),
                    NotificationType.ORDER,
                    title,
                    body,
                    priority
            );
        } catch (Exception e) {
            java.util.logging.Logger.getLogger(OrderServiceImpl.class.getName())
                    .warning("Failed to dispatch order status notification: " + e.getMessage());
        }
    }
}
