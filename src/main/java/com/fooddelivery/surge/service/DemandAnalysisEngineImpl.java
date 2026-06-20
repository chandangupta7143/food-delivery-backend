package com.fooddelivery.surge.service;

import com.fooddelivery.orders.entity.Order;
import com.fooddelivery.orders.entity.OrderStatus;
import com.fooddelivery.orders.repository.OrderRepository;
import com.uber.h3core.H3Core;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;
import java.util.logging.Logger;

@Service
public class DemandAnalysisEngineImpl implements DemandAnalysisEngine {

    private static final Logger LOGGER = Logger.getLogger(DemandAnalysisEngineImpl.class.getName());

    private final RedisTemplate<String, Object> redisTemplate;
    private final OrderRepository orderRepository;
    private final H3Core h3;

    // Simulation/configuration flags for telemetry health
    private boolean searchTelemetryActive = true;
    private boolean cartTelemetryActive = true;

    public DemandAnalysisEngineImpl(RedisTemplate<String, Object> redisTemplate,
                                    OrderRepository orderRepository,
                                    H3Core h3) {
        this.redisTemplate = redisTemplate;
        this.orderRepository = orderRepository;
        this.h3 = h3;
    }

    @Override
    public void registerCheckoutAttempt(String h3Index) {
        addToZSet("zset:checkouts:" + h3Index, 600); // 10 min TTL on ZSet
    }

    @Override
    public void registerCartAddition(String h3Index) {
        addToZSet("zset:carts:" + h3Index, 600);
    }

    @Override
    public void registerOrderPlaced(String h3Index) {
        addToZSet("zset:orders:" + h3Index, 1200); // 20 min TTL on ZSet
    }

    @Override
    public void registerSearchQuery(String h3Index) {
        addToZSet("zset:searches:" + h3Index, 600);
    }

    @Override
    public double calculateDemandScore(String h3Index) {
        long now = System.currentTimeMillis();

        // 1. Fetch raw signal counts
        long checkouts = getRollingCount("zset:checkouts:" + h3Index, 300000); // 5 mins
        long carts = cartTelemetryActive ? getRollingCount("zset:carts:" + h3Index, 600000) : 0L; // 10 mins
        long orders = getRollingCount("zset:orders:" + h3Index, 900000); // 15 mins
        long searches = searchTelemetryActive ? getRollingCount("zset:searches:" + h3Index, 300000) : 0L; // 5 mins
        long pending = getPendingOrderCountInH3(h3Index);

        // 2. Normalize raw counts to scores out of 100
        double sCheckout = Math.min(100.0, checkouts * 10.0);
        double sCart = Math.min(100.0, carts * 5.0);
        double sOrder = Math.min(100.0, orders * 15.0);
        double sSearch = Math.min(100.0, searches * 2.0);
        double sPending = Math.min(100.0, pending * 20.0);

        // 3. Resolve weights based on Fallback Matrix
        double wCheckout, wCart, wOrder, wSearch, wPending;

        if (searchTelemetryActive && cartTelemetryActive) {
            // All operational
            wCheckout = 0.30;
            wCart = 0.20;
            wOrder = 0.25;
            wSearch = 0.10;
            wPending = 0.15;
        } else if (!searchTelemetryActive && cartTelemetryActive) {
            // Fallback Level 1 (Search offline) - reallocate search weight (0.10) to Checkout (+0.05) and Orders (+0.05)
            wCheckout = 0.35;
            wCart = 0.20;
            wOrder = 0.30;
            wSearch = 0.00;
            wPending = 0.15;
            LOGGER.info("Demand Engine running on Fallback Level 1 (Search offline)");
        } else {
            // Fallback Level 2 (Search and Cart offline) - reallocate Cart (0.20) and Search (0.10) to Checkout (+0.15), Orders (+0.10), Pending (+0.05)
            wCheckout = 0.45;
            wCart = 0.00;
            wOrder = 0.35;
            wSearch = 0.00;
            wPending = 0.20;
            LOGGER.info("Demand Engine running on Fallback Level 2 (Search & Cart offline)");
        }

        // 4. Calculate weighted score
        double finalScore = (wCheckout * sCheckout) +
                             (wCart * sCart) +
                             (wOrder * sOrder) +
                             (wSearch * sSearch) +
                             (wPending * sPending);

        return Math.min(100.0, Math.max(0.0, finalScore));
    }

    private void addToZSet(String key, int ttlSeconds) {
        long now = System.currentTimeMillis();
        String member = UUID.randomUUID().toString();
        redisTemplate.opsForZSet().add(key, member, now);
        redisTemplate.expire(key, java.time.Duration.ofSeconds(ttlSeconds));
        
        // Prune stale elements on write to avoid write amplification in hot quote path
        long cutoff = now - (ttlSeconds * 1000L);
        redisTemplate.opsForZSet().removeRangeByScore(key, 0, cutoff);
    }

    private long getRollingCount(String key, long windowMillis) {
        long now = System.currentTimeMillis();
        long cutoff = now - windowMillis;
        // Perform O(log(N)) read-only count without making modifications to the ZSet
        Long count = redisTemplate.opsForZSet().count(key, cutoff, now);
        return count != null ? count : 0L;
    }

    private long getPendingOrderCountInH3(String targetH3Index) {
        List<Order> unassignedOrders = orderRepository.findByStatus(OrderStatus.CREATED);
        long count = 0;
        for (Order order : unassignedOrders) {
            if (order.getRestaurantCoordinates() != null) {
                try {
                    String orderH3 = h3.latLngToCellAddress(
                            order.getRestaurantCoordinates().getY(),
                            order.getRestaurantCoordinates().getX(),
                            8
                    );
                    if (targetH3Index.equalsIgnoreCase(orderH3)) {
                        count++;
                    }
                } catch (Exception e) {
                    // Ignore coordinate parsing errors in lookup loop
                }
            }
        }
        return count;
    }

    // Setters for testing rebalancing matrices
    public void setSearchTelemetryActive(boolean active) {
        this.searchTelemetryActive = active;
    }

    public void setCartTelemetryActive(boolean active) {
        this.cartTelemetryActive = active;
    }
}
