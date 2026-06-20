package com.fooddelivery.delivery.service;

import com.fooddelivery.delivery.entity.DeliveryPartner;
import com.fooddelivery.delivery.entity.DeliveryPartnerStatus;
import com.fooddelivery.orders.entity.EventType;
import com.fooddelivery.orders.entity.Order;
import com.fooddelivery.orders.entity.OrderStatus;
import com.fooddelivery.orders.entity.StatusEvent;
import com.fooddelivery.orders.repository.OrderRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.mongodb.core.FindAndModifyOptions;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.geo.GeoJsonPoint;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.data.mongodb.core.query.Update;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import java.util.logging.Logger;
import java.util.stream.Collectors;

@Component
public class AssignmentEngine {

    private static final Logger logger = Logger.getLogger(AssignmentEngine.class.getName());

    private final MongoTemplate mongoTemplate;
    private final OrderRepository orderRepository;

    @Value("${delivery.assignment.timeout-seconds:30}")
    private int timeoutSeconds;

    @Value("${delivery.assignment.max-attempts:5}")
    private int maxAttempts;

    public AssignmentEngine(MongoTemplate mongoTemplate, OrderRepository orderRepository) {
        this.mongoTemplate = mongoTemplate;
        this.orderRepository = orderRepository;
    }

    /**
     * Entry point: Trigger driver assignment for a ready order.
     */
    public void assignDriver(Order order) {
        if (order.getRestaurantCoordinates() == null) {
            logger.warning("Order " + order.getId() + " is missing restaurant coordinates. Cannot perform geo assignment.");
            return;
        }

        if (order.getAssignmentAttempts() >= maxAttempts) {
            logger.warning("Order " + order.getId() + " exceeded max assignment attempts (" + maxAttempts + "). Requires admin intervention.");
            return;
        }

        GeoJsonPoint restLoc = order.getRestaurantCoordinates();
        double[] radiuses = {3000.0, 5000.0, 8000.0}; // 3km, 5km, 8km

        for (double radius : radiuses) {
            logger.info("Sweeping active drivers within " + (radius / 1000.0) + " km for Order " + order.getId());
            List<DeliveryPartner> candidates = findCandidates(restLoc, radius);

            if (candidates.isEmpty()) {
                continue;
            }

            // Score and rank candidates
            List<ScoredDriver> rankedDrivers = candidates.stream()
                    .map(driver -> scoreDriver(driver, restLoc))
                    .sorted(Comparator.comparingDouble(ScoredDriver::getScore).reversed())
                    .collect(Collectors.toList());

            // Try to acquire atomic lock
            for (ScoredDriver scoredDriver : rankedDrivers) {
                DeliveryPartner driver = scoredDriver.getDriver();
                if (acquireDriverLock(driver.getId(), order.getId())) {
                    logger.info("Successfully matched Driver " + driver.getId() + " to Order " + order.getId() + " with score: " + scoredDriver.getScore());
                    
                    order.setDeliveryPartnerId(driver.getId());
                    order.setAssignmentAttempts(order.getAssignmentAttempts() + 1);
                    order.getStatusHistory().add(new StatusEvent(
                            order.getStatus(),
                            EventType.SYSTEM_EVENT,
                            LocalDateTime.now(),
                            "SYSTEM_ASSIGNMENT",
                            "SYSTEM"
                    ));
                    orderRepository.save(order);
                    return; // Successfully assigned!
                }
            }
        }

        logger.info("No available drivers matched for Order " + order.getId() + " in this pass. Will retry in background.");
    }

    /**
     * Background scheduler to clean up timed-out offers.
     * Runs every 5 seconds.
     */
    @Scheduled(fixedDelay = 5000)
    public void checkForTimeouts() {
        LocalDateTime cutoff = LocalDateTime.now().minusSeconds(timeoutSeconds);

        Query query = new Query(Criteria.where("status").is(DeliveryPartnerStatus.BUSY)
                .and("lastActiveTime").lt(cutoff));

        List<DeliveryPartner> timedOutDrivers = mongoTemplate.find(query, DeliveryPartner.class);

        for (DeliveryPartner driver : timedOutDrivers) {
            logger.info("Driver offer timed out for Driver: " + driver.getId() + " on Order: " + driver.getCurrentOrderId());
            
            // Release driver
            driver.setTotalTimeouts(driver.getTotalTimeouts() + 1);
            driver.setConsecutiveRejections(driver.getConsecutiveRejections() + 1);
            
            if (driver.getConsecutiveRejections() >= 3) {
                driver.setStatus(DeliveryPartnerStatus.SUSPENDED);
                logger.warning("Driver " + driver.getId() + " auto-suspended due to 3 consecutive rejections/timeouts.");
            } else {
                driver.setStatus(DeliveryPartnerStatus.ONLINE);
            }

            String orderId = driver.getCurrentOrderId();
            driver.setCurrentOrderId(null);
            driver.setLastActiveTime(LocalDateTime.now());
            mongoTemplate.save(driver);

            // Update Order
            if (orderId != null) {
                orderRepository.findById(orderId).ifPresent(order -> {
                    order.setDeliveryPartnerId(null);
                    order.getStatusHistory().add(new StatusEvent(
                            order.getStatus(),
                            EventType.SYSTEM_EVENT,
                            LocalDateTime.now(),
                            "SYSTEM",
                            "SYSTEM"
                    ));
                    orderRepository.save(order);
                });
            }
        }
    }

    /**
     * Background scheduler to retry matching unassigned orders.
     * Runs every 15 seconds.
     */
    @Scheduled(fixedDelay = 15000)
    public void assignUnassignedOrders() {
        List<Order> unassignedOrders = orderRepository.findByStatus(OrderStatus.READY_FOR_PICKUP).stream()
                .filter(order -> order.getDeliveryPartnerId() == null && order.getAssignmentAttempts() < maxAttempts)
                .collect(Collectors.toList());

        for (Order order : unassignedOrders) {
            logger.info("Retrying auto-assignment for unassigned Order: " + order.getId());
            assignDriver(order);
        }
    }

    private List<DeliveryPartner> findCandidates(GeoJsonPoint restLoc, double maxDistanceMeters) {
        LocalDateTime minFreshnessTime = LocalDateTime.now().minusSeconds(60);

        Query query = new Query(Criteria.where("status").is(DeliveryPartnerStatus.ONLINE)
                .and("lastLocationUpdateTime").gte(minFreshnessTime)
                .and("currentLocation").nearSphere(restLoc).maxDistance(maxDistanceMeters));

        return mongoTemplate.find(query, DeliveryPartner.class);
    }

    private boolean acquireDriverLock(String driverId, String orderId) {
        Query query = new Query(Criteria.where("id").is(driverId)
                .and("status").is(DeliveryPartnerStatus.ONLINE));

        Update update = new Update()
                .set("status", DeliveryPartnerStatus.BUSY)
                .set("currentOrderId", orderId)
                .set("lastActiveTime", LocalDateTime.now())
                .inc("totalAssignments", 1);

        FindAndModifyOptions options = FindAndModifyOptions.options().returnNew(true);
        DeliveryPartner partner = mongoTemplate.findAndModify(query, update, options, DeliveryPartner.class);
        return partner != null;
    }

    private ScoredDriver scoreDriver(DeliveryPartner driver, GeoJsonPoint restLoc) {
        double dist = calculateDistanceMeters(driver.getCurrentLocation(), restLoc);

        // Distance Score: max 100, drops by 1 for every 100 meters
        double sd = Math.max(0.0, 100.0 - (dist / 100.0));

        // GPS Freshness Score: max 100, drops by 1 for every second elapsed
        long secondsElapsed = 0;
        if (driver.getLastLocationUpdateTime() != null) {
            secondsElapsed = ChronoUnit.SECONDS.between(driver.getLastLocationUpdateTime(), LocalDateTime.now());
        }
        double sf = Math.max(0.0, 100.0 - secondsElapsed);

        // Acceptance Rate Score: raw percentage 0-100
        double sa = driver.getAcceptanceRate();

        // Speed/Time Score: 100 - average delivery time in minutes
        double st = Math.max(0.0, 100.0 - driver.getAverageDeliveryTimeMinutes());

        // Rating Score: rating (1-5) scaled to 0-100
        double sr = driver.getRating() * 20.0;

        // Workload/Fatigue Factor: daily deliveries count times 5
        double sw = driver.getDailyDeliveryCount() * 5.0;

        // Weighted Scoring formula
        double score = (0.40 * sd) + (0.15 * sf) + (0.15 * sa) + (0.10 * st) + (0.10 * sr) - (0.10 * sw);

        return new ScoredDriver(driver, score);
    }

    private double calculateDistanceMeters(GeoJsonPoint p1, GeoJsonPoint p2) {
        if (p1 == null || p2 == null) return 999999.0;
        double rx = p1.getX();
        double ry = p1.getY();
        double dx = p2.getX();
        double dy = p2.getY();
        return Math.sqrt(Math.pow(dx - rx, 2) + Math.pow(dy - ry, 2)) * 111000.0;
    }

    private static class ScoredDriver {
        private final DeliveryPartner driver;
        private final double score;

        public ScoredDriver(DeliveryPartner driver, double score) {
            this.driver = driver;
            this.score = score;
        }

        public DeliveryPartner getDriver() {
            return driver;
        }

        public double getScore() {
            return score;
        }
    }
}
