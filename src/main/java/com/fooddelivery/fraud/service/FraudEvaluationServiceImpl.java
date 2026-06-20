package com.fooddelivery.fraud.service;

import com.fooddelivery.fraud.entity.DailyFraudMetrics;
import com.fooddelivery.fraud.entity.UserRestriction;
import com.fooddelivery.fraud.repository.DailyFraudMetricsRepository;
import com.fooddelivery.fraud.repository.UserRestrictionRepository;
import com.fooddelivery.orders.entity.FraudDetails;
import com.fooddelivery.orders.entity.Order;
import com.fooddelivery.orders.entity.OrderStatus;
import com.fooddelivery.orders.entity.PaymentStatus;
import com.fooddelivery.orders.entity.ReviewStatus;
import com.fooddelivery.orders.entity.TriggeredRule;
import com.fooddelivery.orders.repository.OrderRepository;
import com.fooddelivery.users.repository.UserRepository;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.data.mongodb.core.query.Update;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.*;
import java.util.stream.Collectors;

@Service
public class FraudEvaluationServiceImpl implements FraudEvaluationService {

    private final OrderRepository orderRepository;
    private final UserRepository userRepository;
    private final UserRestrictionRepository userRestrictionRepository;
    private final DailyFraudMetricsRepository dailyMetricsRepository;
    private final MongoTemplate mongoTemplate;

    // Concurrency Lock: ensures one active fraud check per user during checkout.
    // NOTE (Option B - Single Node Lock Intentional Design):
    // In this deployment architecture, the application is provisioned as a single-node service.
    // Backing the lock via a local ConcurrentHashMap registry is intentionally chosen to avoid 
    // the latency overhead and infrastructure complexity of external caching layers (such as Redis/Redisson)
    // while fully guaranteeing atomic checkout prevention on the primary deployment thread pool.
    // If the system scales horizontally in the future, this registry must be refactored to use Redisson.
    private final ConcurrentHashMap<String, Boolean> activeLocks = new ConcurrentHashMap<>();

    // Thread pool for bounding execution within the 500ms SLA
    private final ExecutorService executorService = Executors.newFixedThreadPool(8);

    public FraudEvaluationServiceImpl(OrderRepository orderRepository,
                                      UserRepository userRepository,
                                      UserRestrictionRepository userRestrictionRepository,
                                      DailyFraudMetricsRepository dailyMetricsRepository,
                                      MongoTemplate mongoTemplate) {
        this.orderRepository = orderRepository;
        this.userRepository = userRepository;
        this.userRestrictionRepository = userRestrictionRepository;
        this.dailyMetricsRepository = dailyMetricsRepository;
        this.mongoTemplate = mongoTemplate;
    }

    @Override
    public void evaluateOrder(Order order) {
        String userId = order.getUserId();
        if (userId == null) {
            return;
        }

        // 1. Enforce Concurrency Lock during checkout
        if (activeLocks.putIfAbsent(userId, Boolean.TRUE) != null) {
            throw new IllegalStateException("Another checkout validation is already in progress for this user.");
        }

        try {
            // 2. Pre-check: Block instantly if user has an active BLOCK_ORDERING restriction
            Optional<UserRestriction> activeRest = userRestrictionRepository.findByUserId(userId);
            if (activeRest.isPresent()) {
                UserRestriction restriction = activeRest.get();
                if (restriction.getExpiresAt().isAfter(LocalDateTime.now()) &&
                        restriction.getRestrictionTypes().contains("BLOCK_ORDERING")) {
                    throw new SecurityException("Your account is restricted from placing new orders due to previous violations.");
                }
            }

            // 3. Execute evaluation inside the 500ms SLA timeout window
            Future<?> future = executorService.submit(() -> runEvaluationRules(order));
            try {
                future.get(500, TimeUnit.MILLISECONDS);
            } catch (TimeoutException e) {
                future.cancel(true);
                applyTimeoutFallback(order, "Evaluation timeout");
            } catch (Exception e) {
                applyTimeoutFallback(order, "Rule Engine execution exception: " + e.getMessage());
            }
        } finally {
            activeLocks.remove(userId);
        }
    }

    private void runEvaluationRules(Order order) {
        String userId = order.getUserId();
        LocalDateTime now = LocalDateTime.now();

        double riskScore = 0.0;
        List<String> flags = new ArrayList<>();
        List<TriggeredRule> triggeredRules = new ArrayList<>();

        // Fetch user transaction history (last 30 days)
        List<Order> orders = orderRepository.findByUserIdAndCreatedAtAfter(userId, now.minusDays(30));

        // 1. HIGH_VELOCITY_ORDERS (Weight: 15%)
        long velocityCount = orders.stream()
                .filter(o -> o.getCreatedAt() != null && o.getCreatedAt().isAfter(now.minusMinutes(15)))
                .count();
        if (velocityCount > 2) {
            double pts = velocityCount > 4 ? 60.0 : 30.0;
            riskScore += pts;
            flags.add("MULTIPLE_ORDERS");
            triggeredRules.add(new TriggeredRule("HIGH_VELOCITY_ORDERS", pts));
        }

        // 2. EXCESSIVE_CANCELLATIONS (Weight: 15%)
        List<Order> last24hOrders = orders.stream()
                .filter(o -> o.getCreatedAt() != null && o.getCreatedAt().isAfter(now.minusHours(24)))
                .collect(Collectors.toList());
        if (last24hOrders.size() >= 3) {
            long cancelledCount = last24hOrders.stream()
                    .filter(o -> o.getStatus() == OrderStatus.CANCELLED)
                    .count();
            double ratio = (double) cancelledCount / last24hOrders.size();
            if (ratio > 0.4) {
                riskScore += 40.0;
                flags.add("EXCESSIVE_CANCELLATIONS");
                triggeredRules.add(new TriggeredRule("EXCESSIVE_CANCELLATIONS", 40.0));
            }
        }

        // 3. EXCESSIVE_REFUNDS (Weight: 20%)
        List<Order> last7dOrders = orders.stream()
                .filter(o -> o.getCreatedAt() != null && o.getCreatedAt().isAfter(now.minusDays(7)))
                .collect(Collectors.toList());
        if (last7dOrders.size() >= 2) {
            long refundCount = last7dOrders.stream()
                    .filter(o -> o.getPaymentStatus() == PaymentStatus.REFUNDED)
                    .count();
            double ratio = (double) refundCount / last7dOrders.size();
            if (ratio > 0.5) {
                riskScore += 50.0;
                flags.add("REFUND_ABUSE");
                triggeredRules.add(new TriggeredRule("EXCESSIVE_REFUNDS", 50.0));
            }
        }

        // 4. COUPON_ABUSE (Weight: 15%)
        // Coupon abuse detection proxy: check if multiple different accounts ordered to same address using a coupon in 30 days
        if (order.getPricing() != null && order.getPricing().getDiscount() > 0.0) {
            String currentAddress = order.getDeliveryAddress();
            if (currentAddress != null && !currentAddress.trim().isEmpty()) {
                List<Order> addressOrders = orderRepository.findByDeliveryAddressIgnoreCaseAndCreatedAtAfterAndPricingDiscountGreaterThan(
                        currentAddress, now.minusDays(30), 0.0);
                long distinctUsers = addressOrders.stream().map(Order::getUserId).distinct().count();
                if (distinctUsers > 2) {
                    riskScore += 45.0;
                    flags.add("COUPON_ABUSE");
                    triggeredRules.add(new TriggeredRule("COUPON_ABUSE", 45.0));
                }
            }
        }

        // 5. NEW_USER_HIGH_VALUE (Weight: 10%)
        com.fooddelivery.users.entity.User user = userRepository.findById(userId).orElse(null);
        if (user != null && user.getCreatedAt() != null && user.getCreatedAt().isAfter(now.minusHours(1))) {
            if (order.getPricing() != null && order.getPricing().getFinalPayable() > 3000.0) {
                riskScore += 30.0;
                flags.add("HIGH_RISK_NEW_USER");
                triggeredRules.add(new TriggeredRule("NEW_USER_HIGH_VALUE", 30.0));
            }
        }

        // 6. RAPID_REPEATED_ORDER (Weight: 10%)
        long rapidCount = orders.stream()
                .filter(o -> o.getCreatedAt() != null && o.getCreatedAt().isAfter(now.minusSeconds(120)))
                .count();
        if (rapidCount > 0) {
            riskScore += 40.0;
            flags.add("SUSPICIOUS_ACTIVITY");
            triggeredRules.add(new TriggeredRule("RAPID_REPEATED_ORDER", 40.0));
        }

        // 7. GEO_MISMATCH (Weight: 10%)
        if (order.getDeliveryCoordinates() != null && order.getRestaurantCoordinates() != null) {
            double rx = order.getRestaurantCoordinates().getX();
            double ry = order.getRestaurantCoordinates().getY();
            double dx = order.getDeliveryCoordinates().getX();
            double dy = order.getDeliveryCoordinates().getY();
            double dist = haversine(rx, ry, dx, dy);
            if (dist > 100.0) {
                riskScore += 35.0;
                flags.add("SUSPICIOUS_ACTIVITY");
                triggeredRules.add(new TriggeredRule("GEO_MISMATCH", 35.0));
            }
        }

        // 8. FAILED_PAYMENT_LOOP (Weight: 15%)
        long failedPayments = orders.stream()
                .filter(o -> o.getCreatedAt() != null &&
                        o.getCreatedAt().isAfter(now.minusMinutes(10)) &&
                        o.getPaymentStatus() == PaymentStatus.FAILED)
                .count();
        if (failedPayments >= 3) {
            riskScore += 50.0;
            flags.add("FAILED_PAYMENT_PATTERN");
            triggeredRules.add(new TriggeredRule("FAILED_PAYMENT_LOOP", 50.0));
        }

        // Normalize final score
        riskScore = Math.min(100.0, Math.max(0.0, riskScore));

        // Write fraud evaluation state
        FraudDetails fraudDetails = order.getFraudDetails();
        if (fraudDetails == null) {
            fraudDetails = new FraudDetails();
            order.setFraudDetails(fraudDetails);
        }
        fraudDetails.setRiskScore(riskScore);
        fraudDetails.setFraudFlags(flags);
        fraudDetails.setTriggeredRules(triggeredRules);
        fraudDetails.setRuleVersion("FRAUD_RULESET_V1");
        order.setFraudCheckedAt(now);
        order.setFraudDecisionBy("SYSTEM_FRAUD_SHIELD");

        // Action outcomes based on scoring ranges
        if (riskScore >= 80.0) {
            fraudDetails.setReviewStatus(ReviewStatus.REJECTED);
            order.setStatus(OrderStatus.REJECTED);
            order.setPaymentStatus(PaymentStatus.FAILED);
            // Apply Escalation Matrix
            applyUserRestriction(userId, "Auto-rejected due to critical risk score of " + riskScore);
        } else if (riskScore >= 60.0) {
            fraudDetails.setReviewStatus(ReviewStatus.MANUAL_REVIEW);
            order.setStatus(OrderStatus.PENDING_REVIEW);
        } else {
            fraudDetails.setReviewStatus(ReviewStatus.PASSED);
            order.setStatus(OrderStatus.CREATED);
        }

        incrementDailyMetrics(riskScore, flags.contains("COUPON_ABUSE"));
    }

    private void applyTimeoutFallback(Order order, String notes) {
        LocalDateTime now = LocalDateTime.now();
        FraudDetails fraudDetails = order.getFraudDetails();
        if (fraudDetails == null) {
            fraudDetails = new FraudDetails();
            order.setFraudDetails(fraudDetails);
        }
        fraudDetails.setRiskScore(75.0); // Held state
        fraudDetails.setFraudFlags(List.of("EVALUATION_TIMEOUT"));
        fraudDetails.setTriggeredRules(List.of(new TriggeredRule("EVALUATION_TIMEOUT", 75.0)));
        fraudDetails.setRuleVersion("FRAUD_RULESET_V1");
        order.setFraudCheckedAt(now);
        fraudDetails.setReviewStatus(ReviewStatus.MANUAL_REVIEW);
        order.setFraudDecisionBy("SYSTEM_FRAUD_SHIELD");
        fraudDetails.setAdminReviewNotes(notes);

        order.setStatus(OrderStatus.PENDING_REVIEW);
        incrementDailyMetrics(75.0, false);
    }

    private void applyUserRestriction(String userId, String reason) {
        // Query the number of rejections for this user in order history
        long rejections = orderRepository.countByUserIdAndFraudDetailsReviewStatus(userId, ReviewStatus.REJECTED);

        List<String> restrictionTypes = new ArrayList<>();
        LocalDateTime expiresAt = LocalDateTime.now();

        // 1st offense (rejections == 0 because current rejection is not saved yet): warning
        // 2nd offense (rejections == 1): coupon restriction (7 days)
        // 3rd offense (rejections == 2): refund restriction (30 days)
        // 4th offense (rejections >= 3): ordering restriction (365 days)
        if (rejections == 0) {
            restrictionTypes.add("WARNING");
            expiresAt = expiresAt.plusDays(365);
        } else if (rejections == 1) {
            restrictionTypes.add("BLOCK_COUPONS");
            expiresAt = expiresAt.plusDays(7);
        } else if (rejections == 2) {
            restrictionTypes.add("BLOCK_REFUNDS");
            expiresAt = expiresAt.plusDays(30);
        } else {
            restrictionTypes.add("BLOCK_ORDERING");
            expiresAt = expiresAt.plusDays(365);
        }

        UserRestriction restriction = userRestrictionRepository.findByUserId(userId)
                .orElse(new UserRestriction(userId, new ArrayList<>(), reason, expiresAt, "SYSTEM_FRAUD_SHIELD"));

        for (String type : restrictionTypes) {
            if (!restriction.getRestrictionTypes().contains(type)) {
                restriction.getRestrictionTypes().add(type);
            }
        }
        restriction.setExpiresAt(expiresAt);
        restriction.setReason(reason);
        restriction.setCreatedAt(LocalDateTime.now());
        userRestrictionRepository.save(restriction);
    }

    private void incrementDailyMetrics(double riskScore, boolean isCouponAbuse) {
        String today = LocalDate.now().toString();

        Query query = new Query(Criteria.where("id").is(today));
        Update update = new Update();

        update.inc("totalOrdersProcessed", 1);
        if (riskScore >= 80.0) {
            update.inc("totalAutoRejected", 1);
        } else if (riskScore >= 60.0) {
            update.inc("totalHoldForReview", 1);
        }
        if (isCouponAbuse) {
            update.inc("totalPromoBlocked", 1);
        }
        update.set("lastUpdatedAt", LocalDateTime.now());

        mongoTemplate.upsert(query, update, DailyFraudMetrics.class);
    }

    private double haversine(double lon1, double lat1, double lon2, double lat2) {
        double dLat = Math.toRadians(lat2 - lat1);
        double dLon = Math.toRadians(lon2 - lon1);
        double a = Math.sin(dLat / 2) * Math.sin(dLat / 2) +
                Math.cos(Math.toRadians(lat1)) * Math.cos(Math.toRadians(lat2)) *
                Math.sin(dLon / 2) * Math.sin(dLon / 2);
        double c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));
        return 6371.0 * c;
    }
}
