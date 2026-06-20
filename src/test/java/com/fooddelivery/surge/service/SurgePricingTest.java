package com.fooddelivery.surge.service;

import com.fooddelivery.orders.entity.Order;
import com.fooddelivery.orders.entity.OrderStatus;
import com.fooddelivery.orders.repository.OrderRepository;
import com.fooddelivery.surge.dto.PricingQuoteRequest;
import com.fooddelivery.surge.dto.PricingQuoteResponse;
import com.fooddelivery.surge.entity.DailySurgeSummary;
import com.fooddelivery.surge.entity.SurgeRule;
import com.fooddelivery.surge.repository.DailySurgeSummaryRepository;
import com.fooddelivery.surge.repository.SurgeOverrideRepository;
import com.fooddelivery.surge.repository.SurgeRuleRepository;
import com.fooddelivery.users.repository.UserRepository;
import com.uber.h3core.H3Core;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.ValueOperations;
import org.springframework.data.redis.core.ZSetOperations;
import org.springframework.data.redis.core.SetOperations;
import org.springframework.data.redis.core.GeoOperations;
import org.springframework.data.mongodb.core.geo.GeoJsonPoint;

import java.io.IOException;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class SurgePricingTest {

    @Mock
    private ValueOperations<String, Object> valueOperations;

    @Mock
    private ZSetOperations<String, Object> zSetOperations;

    @Mock
    private SetOperations<String, Object> setOperations;

    @Mock
    private GeoOperations<String, Object> geoOperations;

    @Mock
    private OrderRepository orderRepository;

    @Mock
    private SurgeRuleRepository surgeRuleRepository;

    @Mock
    private SurgeOverrideRepository surgeOverrideRepository;

    @Mock
    private DailySurgeSummaryRepository dailySurgeSummaryRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private org.springframework.data.mongodb.core.MongoTemplate mongoTemplate;

    @Mock
    private DemandAnalysisEngine demandEngine;

    @Mock
    private SupplyAnalysisEngine supplyEngine;

    @Mock
    private SurgeCalculationEngine calculationEngine;

    @Mock
    private PricingDecisionEngine decisionEngine;

    private H3Core h3;
    private HmacTokenService tokenService;
    private TestRedisTemplate redisTemplate;

    private static class TestRedisTemplate extends RedisTemplate<String, Object> {
        private final ValueOperations<String, Object> valueOps;
        private final ZSetOperations<String, Object> zSetOps;
        private final SetOperations<String, Object> setOps;
        private final GeoOperations<String, Object> geoOps;
        private final java.util.Map<String, Object> mockStore = new java.util.HashMap<>();
        private final java.util.Set<String> deletedKeys = new java.util.HashSet<>();

        public TestRedisTemplate(ValueOperations<String, Object> valueOps,
                                 ZSetOperations<String, Object> zSetOps,
                                 SetOperations<String, Object> setOps,
                                 GeoOperations<String, Object> geoOps) {
            this.valueOps = valueOps;
            this.zSetOps = zSetOps;
            this.setOps = setOps;
            this.geoOps = geoOps;
        }

        @Override
        public <T> T execute(org.springframework.data.redis.core.script.RedisScript<T> script, List<String> keys, Object... args) {
            if (keys != null && !keys.isEmpty()) {
                String key = keys.get(0);
                mockStore.remove(key);
                deletedKeys.add(key);
            }
            return null;
        }

        @Override
        public ValueOperations<String, Object> opsForValue() {
            return valueOps;
        }

        @Override
        public ZSetOperations<String, Object> opsForZSet() {
            return zSetOps;
        }

        @Override
        public SetOperations<String, Object> opsForSet() {
            return setOps;
        }

        @Override
        public GeoOperations<String, Object> opsForGeo() {
            return geoOps;
        }

        @Override
        public Boolean hasKey(String key) {
            return mockStore.containsKey(key);
        }

        @Override
        public Boolean delete(String key) {
            mockStore.remove(key);
            deletedKeys.add(key);
            return true;
        }

        @Override
        public Boolean expire(String key, java.time.Duration timeout) {
            return true;
        }

        public void putMock(String key, Object val) {
            mockStore.put(key, val);
        }

        public boolean isDeleted(String key) {
            return deletedKeys.contains(key);
        }
    }

    @BeforeEach
    void setUp() throws IOException {
        redisTemplate = new TestRedisTemplate(valueOperations, zSetOperations, setOperations, geoOperations);
        tokenService = new HmacTokenService("my-secure-shared-secret-key-32-bytes", redisTemplate);
        h3 = H3Core.newInstance();
    }

    @Test
    void testHmacTokenService_GenerateAndVerify() {
        String userId = "user_customer_9981";
        String restaurantId = "rest_1";
        String h3Index = "8861892513fffff";
        double surgeMultiplier = 1.45;
        double deliveryFee = 43.50;
        long expiresAt = (System.currentTimeMillis() / 1000) + 120; // 120s TTL

        String token = tokenService.generateToken(userId, restaurantId, h3Index, surgeMultiplier, deliveryFee, expiresAt);
        assertNotNull(token);
        assertTrue(token.contains("."));

        boolean isValid = tokenService.verifyToken(token, userId, restaurantId, h3Index, surgeMultiplier, deliveryFee);
        assertTrue(isValid);
    }

    @Test
    void testHmacTokenService_TamperingDetection() {
        String userId = "user_customer_9981";
        String restaurantId = "rest_1";
        String h3Index = "8861892513fffff";
        double surgeMultiplier = 1.45;
        double deliveryFee = 43.50;
        long expiresAt = (System.currentTimeMillis() / 1000) + 120;

        String token = tokenService.generateToken(userId, restaurantId, h3Index, surgeMultiplier, deliveryFee, expiresAt);

        // Tamper user ID
        assertFalse(tokenService.verifyToken(token, "attacker_user", restaurantId, h3Index, surgeMultiplier, deliveryFee));
        // Tamper restaurant ID
        assertFalse(tokenService.verifyToken(token, userId, "rest_2", h3Index, surgeMultiplier, deliveryFee));
        // Tamper H3 index
        assertFalse(tokenService.verifyToken(token, userId, restaurantId, "8861892513aaaaa", surgeMultiplier, deliveryFee));
        // Tamper multiplier
        assertFalse(tokenService.verifyToken(token, userId, restaurantId, h3Index, 2.0, deliveryFee));
        // Tamper fee
        assertFalse(tokenService.verifyToken(token, userId, restaurantId, h3Index, surgeMultiplier, 100.0));
    }

    @Test
    void testHmacTokenService_Expiration() {
        String userId = "user_customer_9981";
        String restaurantId = "rest_1";
        String h3Index = "8861892513fffff";
        double surgeMultiplier = 1.45;
        double deliveryFee = 43.50;
        long expiresAt = (System.currentTimeMillis() / 1000) - 10; // Expired 10s ago

        String token = tokenService.generateToken(userId, restaurantId, h3Index, surgeMultiplier, deliveryFee, expiresAt);
        assertFalse(tokenService.verifyToken(token, userId, restaurantId, h3Index, surgeMultiplier, deliveryFee));
    }

    @Test
    void testDemandAnalysisEngine_FallbackMatrix() {
        DemandAnalysisEngineImpl demandEngineImpl = new DemandAnalysisEngineImpl(redisTemplate, orderRepository, h3);

        String targetH3Index = h3.latLngToCellAddress(28.6282, 77.3898, 8);

        // Simulate telemetry counts via read-only ZCOUNT stubbing
        when(zSetOperations.count(contains("checkouts"), anyDouble(), anyDouble())).thenReturn(5L); // 5 checkouts
        when(zSetOperations.count(contains("carts"), anyDouble(), anyDouble())).thenReturn(10L); // 10 carts
        when(zSetOperations.count(contains("orders"), anyDouble(), anyDouble())).thenReturn(2L); // 2 orders
        when(zSetOperations.count(contains("searches"), anyDouble(), anyDouble())).thenReturn(50L); // 50 searches

        // Mock pending order coordinate conversions
        Order unassignedOrder = new Order();
        unassignedOrder.setStatus(OrderStatus.CREATED);
        unassignedOrder.setRestaurantCoordinates(new GeoJsonPoint(77.3898, 28.6282));
        when(orderRepository.findByStatus(OrderStatus.CREATED)).thenReturn(List.of(unassignedOrder));

        // Test normal operational weight mapping
        double scoreNormal = demandEngineImpl.calculateDemandScore(targetH3Index);
        assertTrue(scoreNormal > 0.0 && scoreNormal <= 100.0);

        // Test Fallback Level 1 (Search offline)
        demandEngineImpl.setSearchTelemetryActive(false);
        double scoreFallback1 = demandEngineImpl.calculateDemandScore(targetH3Index);
        assertTrue(scoreFallback1 > 0.0);

        // Test Fallback Level 2 (Search and Cart offline)
        demandEngineImpl.setCartTelemetryActive(false);
        double scoreFallback2 = demandEngineImpl.calculateDemandScore(targetH3Index);
        assertTrue(scoreFallback2 > 0.0);
    }

    @Test
    void testSupplyAnalysisEngine_Calculations() {
        SupplyAnalysisEngineImpl supplyEngineImpl = new SupplyAnalysisEngineImpl(redisTemplate, h3);

        String targetH3Index = h3.latLngToCellAddress(28.6282, 77.3898, 8);

        java.util.Set<Object> driverSet = new java.util.HashSet<>(java.util.Arrays.asList("d1", "d2", "d3", "d4", "d5"));
        when(setOperations.members("set:drivers:h3_res8:" + targetH3Index)).thenReturn(driverSet);
        for (String d : java.util.Arrays.asList("d1", "d2", "d3", "d4", "d5")) {
            redisTemplate.putMock("driver:presence:" + d, "ONLINE");
        }

        long count = supplyEngineImpl.getAvailableDriversCount(targetH3Index);
        assertEquals(5L, count);

        // Calculate supply score
        double score = supplyEngineImpl.calculateSupplyScore(targetH3Index, 2); // 2 pending orders
        // Ss = max(0, 100 - (2 / 5) * 100) = 60.0
        assertEquals(60.0, score, 0.01);

        // Calculate Driver Pressure Index
        double dpi = supplyEngineImpl.calculateDriverPressureIndex(targetH3Index, 2, 8); // 2 pending, 8 checkouts
        // DPI = (2 + 8 * 0.25) / 5 = 4 / 5 = 0.8
        assertEquals(0.8, dpi, 0.01);
    }

    @Test
    void testSurgePricingService_SLA_Under50ms() {
        SurgePricingServiceImpl pricingService = new SurgePricingServiceImpl(
                redisTemplate, surgeRuleRepository, surgeOverrideRepository,
                dailySurgeSummaryRepository, userRepository, mongoTemplate, demandEngine,
                supplyEngine, calculationEngine, decisionEngine, tokenService, h3
        );

        String h3Index = h3.latLngToCellAddress(28.6282, 77.3898, 8);

        when(surgeOverrideRepository.findByH3IndexAndIsActiveTrueAndExpiresAtAfter(anyString(), any(LocalDateTime.class)))
                .thenReturn(Collections.emptyList());
        when(valueOperations.get("surge:h3:" + h3Index)).thenReturn("1.45"); // cache hit

        PricingQuoteRequest quoteReq = new PricingQuoteRequest();
        quoteReq.setCartId("cart_abc");
        quoteReq.setRestaurantId("rest_1");
        quoteReq.setDeliveryLatitude(28.6282);
        quoteReq.setDeliveryLongitude(77.3898);

        long startTime = System.nanoTime();
        PricingQuoteResponse quoteRes = pricingService.calculateQuote(quoteReq);
        long durationMs = (System.nanoTime() - startTime) / 1_000_000;

        assertNotNull(quoteRes);
        assertEquals(1.45, quoteRes.getSurgeMultiplier());
        // Verify SLA: Must take less than 50 milliseconds
        assertTrue(durationMs < 50, "Quote calculation exceeded 50ms SLA: " + durationMs + "ms");
    }

    @Test
    void testSurgePricingService_CacheStampedeMutex() throws Exception {
        SurgePricingServiceImpl pricingService = new SurgePricingServiceImpl(
                redisTemplate, surgeRuleRepository, surgeOverrideRepository,
                dailySurgeSummaryRepository, userRepository, mongoTemplate, demandEngine,
                supplyEngine, calculationEngine, decisionEngine, tokenService, h3
        );

        String h3Index = h3.latLngToCellAddress(28.6282, 77.3898, 8);
        // Simulate cache miss on first call (null value in mockStore)
        
        // Simulate acquiring Redisson-style mutex
        String lockKey = "lock:surge:h3:" + h3Index;
        when(valueOperations.setIfAbsent(eq(lockKey), anyString(), any(java.time.Duration.class)))
                .thenReturn(true); // Thread wins lock

        // Mock calculation parameters
        when(demandEngine.calculateDemandScore(h3Index)).thenReturn(40.0);
        when(setOperations.size(anyString())).thenReturn(2L);
        when(supplyEngine.calculateSupplyScore(eq(h3Index), anyLong())).thenReturn(60.0);
        when(zSetOperations.zCard(anyString())).thenReturn(5L);
        when(supplyEngine.calculateDriverPressureIndex(eq(h3Index), anyLong(), anyLong())).thenReturn(1.2);

        SurgeRule rule = new SurgeRule();
        rule.setThresholdSurge(30.0);
        rule.setScaleFactor(50.0);
        rule.setMaxMultiplier(3.0);
        rule.setBaseMultiplier(1.0);
        when(surgeRuleRepository.findByZoneName("GLOBAL_DEFAULT")).thenReturn(Optional.of(rule));

        when(calculationEngine.calculateSurgeMultiplier(anyString(), anyDouble(), anyDouble(), anyDouble(), anyDouble(), anyDouble(), anyDouble(), anyDouble()))
                .thenReturn(1.5);
        when(decisionEngine.getSmoothedMultiplier(eq(h3Index), eq(1.5), anyDouble())).thenReturn(1.35);

        // Get multiplier
        double multiplier = pricingService.getActiveSurgeMultiplier(h3Index);
        assertEquals(1.35, multiplier);

        // Verify lock is acquired, cache key is set, and lock is deleted
        verify(valueOperations, times(1)).setIfAbsent(eq(lockKey), anyString(), any(java.time.Duration.class));
        verify(valueOperations, times(1)).set(eq("surge:h3:" + h3Index), eq(1.35), any(java.time.Duration.class));
        assertTrue(redisTemplate.isDeleted(lockKey));
    }
}
