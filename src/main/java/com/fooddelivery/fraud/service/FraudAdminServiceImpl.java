package com.fooddelivery.fraud.service;

import com.fooddelivery.fraud.entity.DailyFraudMetrics;
import com.fooddelivery.fraud.entity.FraudAuditLog;
import com.fooddelivery.fraud.entity.UserRestriction;
import com.fooddelivery.fraud.repository.DailyFraudMetricsRepository;
import com.fooddelivery.fraud.repository.FraudAuditLogRepository;
import com.fooddelivery.fraud.repository.UserRestrictionRepository;
import com.fooddelivery.orders.dto.OrderResponse;
import com.fooddelivery.orders.entity.*;
import com.fooddelivery.orders.mapper.OrderMapper;
import com.fooddelivery.orders.repository.OrderRepository;
import com.fooddelivery.users.entity.User;
import com.fooddelivery.users.repository.UserRepository;
import com.fooddelivery.notifications.service.NotificationService;
import com.fooddelivery.notifications.entity.NotificationType;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
public class FraudAdminServiceImpl implements FraudAdminService {

    private final OrderRepository orderRepository;
    private final UserRepository userRepository;
    private final UserRestrictionRepository userRestrictionRepository;
    private final DailyFraudMetricsRepository dailyMetricsRepository;
    private final FraudAuditLogRepository auditLogRepository;
    private final OrderMapper orderMapper;
    private final NotificationService notificationService;

    public FraudAdminServiceImpl(OrderRepository orderRepository,
                                 UserRepository userRepository,
                                 UserRestrictionRepository userRestrictionRepository,
                                 DailyFraudMetricsRepository dailyMetricsRepository,
                                 FraudAuditLogRepository auditLogRepository,
                                 OrderMapper orderMapper,
                                 NotificationService notificationService) {
        this.orderRepository = orderRepository;
        this.userRepository = userRepository;
        this.userRestrictionRepository = userRestrictionRepository;
        this.dailyMetricsRepository = dailyMetricsRepository;
        this.auditLogRepository = auditLogRepository;
        this.orderMapper = orderMapper;
        this.notificationService = notificationService;
    }

    @Override
    public List<OrderResponse> getFraudQueue() {
        return orderRepository.findByStatus(OrderStatus.PENDING_REVIEW).stream()
                .map(orderMapper::toResponse)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional
    public OrderResponse approveOrder(String orderId, String adminEmail, String reason) {
        // 1. Validate override reason
        validateOverrideReason(reason);

        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new IllegalArgumentException("Order not found"));

        if (order.getStatus() != OrderStatus.PENDING_REVIEW) {
            throw new IllegalStateException("Order is not in PENDING_REVIEW state");
        }

        User admin = userRepository.findByEmail(adminEmail)
                .orElseThrow(() -> new IllegalArgumentException("Admin user not found"));

        LocalDateTime now = LocalDateTime.now();

        // Update Fraud details to passed
        FraudDetails fraudDetails = order.getFraudDetails();
        if (fraudDetails == null) {
            fraudDetails = new FraudDetails();
            order.setFraudDetails(fraudDetails);
        }
        fraudDetails.setReviewStatus(ReviewStatus.PASSED);
        fraudDetails.setAdminOverrideReason(reason);
        order.setFraudDecisionBy(admin.getEmail());
        order.setFraudCheckedAt(now);

        order.setStatus(OrderStatus.CREATED);
        order.setPaymentStatus(PaymentStatus.PAID);

        // Add to order history
        StatusEvent event = new StatusEvent(OrderStatus.CREATED, EventType.ADMIN_OVERRIDE, now, admin.getId(), "ADMIN");
        order.getStatusHistory().add(event);

        order = orderRepository.save(order);

        // Record Audit Log
        FraudAuditLog auditLog = new FraudAuditLog(orderId, order.getUserId(), "APPROVE", adminEmail, reason);
        auditLogRepository.save(auditLog);

        // Send notification
        sendOrderStatusNotification(order, "Order Approved", "Your order has been verified and approved! Chef is preparing now.");

        return orderMapper.toResponse(order);
    }

    @Override
    @Transactional
    public OrderResponse rejectOrder(String orderId, String adminEmail, String reason) {
        // 1. Validate override reason
        validateOverrideReason(reason);

        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new IllegalArgumentException("Order not found"));

        if (order.getStatus() != OrderStatus.PENDING_REVIEW) {
            throw new IllegalStateException("Order is not in PENDING_REVIEW state");
        }

        User admin = userRepository.findByEmail(adminEmail)
                .orElseThrow(() -> new IllegalArgumentException("Admin user not found"));

        LocalDateTime now = LocalDateTime.now();

        // Update Fraud details to rejected
        FraudDetails fraudDetails = order.getFraudDetails();
        if (fraudDetails == null) {
            fraudDetails = new FraudDetails();
            order.setFraudDetails(fraudDetails);
        }
        fraudDetails.setReviewStatus(ReviewStatus.REJECTED);
        fraudDetails.setAdminOverrideReason(reason);
        order.setFraudDecisionBy(admin.getEmail());
        order.setFraudCheckedAt(now);

        order.setStatus(OrderStatus.REJECTED);
        order.setPaymentStatus(PaymentStatus.REFUNDED); // Void payment hold

        // Add to order history
        StatusEvent event = new StatusEvent(OrderStatus.REJECTED, EventType.ADMIN_OVERRIDE, now, admin.getId(), "ADMIN");
        order.getStatusHistory().add(event);

        order = orderRepository.save(order);

        // Record Audit Log
        FraudAuditLog auditLog = new FraudAuditLog(orderId, order.getUserId(), "REJECT", adminEmail, reason);
        auditLogRepository.save(auditLog);

        // Send notification
        sendOrderStatusNotification(order, "Order Rejected", "Your order could not be verified and was rejected.");

        return orderMapper.toResponse(order);
    }

    @Override
    @Transactional
    public void restrictUser(String userId, String adminEmail, List<String> restrictionTypes, String reason, int durationDays) {
        if (restrictionTypes == null || restrictionTypes.isEmpty()) {
            throw new IllegalArgumentException("At least one restriction type must be specified");
        }

        LocalDateTime expiresAt = LocalDateTime.now().plusDays(durationDays);
        UserRestriction restriction = userRestrictionRepository.findByUserId(userId)
                .orElse(new UserRestriction(userId, new ArrayList<>(), reason, expiresAt, adminEmail));

        for (String type : restrictionTypes) {
            if (!restriction.getRestrictionTypes().contains(type)) {
                restriction.getRestrictionTypes().add(type);
            }
        }
        restriction.setExpiresAt(expiresAt);
        restriction.setReason(reason);
        restriction.setCreatedBy(adminEmail);
        restriction.setCreatedAt(LocalDateTime.now());
        userRestrictionRepository.save(restriction);
    }

    @Override
    public DailyFraudMetrics getDailyMetrics(String dateStr) {
        if (dateStr == null) {
            dateStr = LocalDate.now().toString();
        }
        return dailyMetricsRepository.findById(dateStr)
                .orElse(new DailyFraudMetrics(dateStr));
    }

    private void validateOverrideReason(String reason) {
        if (reason == null || reason.trim().length() < 10) {
            throw new IllegalArgumentException("adminOverrideReason is mandatory and must be at least 10 characters long");
        }
    }

    private void sendOrderStatusNotification(Order order, String title, String body) {
        try {
            notificationService.sendNotification(
                    order.getUserId(),
                    UUID.randomUUID().toString(),
                    NotificationType.ORDER,
                    title,
                    body,
                    "HIGH"
            );
        } catch (Exception e) {
            java.util.logging.Logger.getLogger(FraudAdminServiceImpl.class.getName())
                    .warning("Failed to dispatch manual review notification: " + e.getMessage());
        }
    }
}
