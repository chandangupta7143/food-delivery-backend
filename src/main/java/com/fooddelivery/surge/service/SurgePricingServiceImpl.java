package com.fooddelivery.surge.service;

import com.fooddelivery.surge.dto.*;
import com.fooddelivery.surge.entity.DailySurgeSummary;
import com.fooddelivery.surge.entity.SurgeOverride;
import com.fooddelivery.surge.entity.SurgeRule;
import com.fooddelivery.surge.repository.DailySurgeSummaryRepository;
import com.fooddelivery.surge.repository.SurgeOverrideRepository;
import com.fooddelivery.surge.repository.SurgeRuleRepository;
import com.fooddelivery.users.entity.User;
import com.fooddelivery.users.repository.UserRepository;
import com.uber.h3core.H3Core;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.logging.Logger;

@Service
public class SurgePricingServiceImpl implements SurgePricingService {

    private static final Logger LOGGER = Logger.getLogger(SurgePricingServiceImpl.class.getName());

    private final RedisTemplate<String, Object> redisTemplate;
    private final SurgeRuleRepository surgeRuleRepository;
    private final SurgeOverrideRepository surgeOverrideRepository;
    private final DailySurgeSummaryRepository dailySurgeSummaryRepository;
    private final UserRepository userRepository;
    private final org.springframework.data.mongodb.core.MongoTemplate mongoTemplate;
    
    private final DemandAnalysisEngine demandEngine;
    private final SupplyAnalysisEngine supplyEngine;
    private final SurgeCalculationEngine calculationEngine;
    private final PricingDecisionEngine decisionEngine;
    private final HmacTokenService tokenService;
    private final H3Core h3;

    public SurgePricingServiceImpl(RedisTemplate<String, Object> redisTemplate,
                                   SurgeRuleRepository surgeRuleRepository,
                                   SurgeOverrideRepository surgeOverrideRepository,
                                   DailySurgeSummaryRepository dailySurgeSummaryRepository,
                                   UserRepository userRepository,
                                   org.springframework.data.mongodb.core.MongoTemplate mongoTemplate,
                                   DemandAnalysisEngine demandEngine,
                                   SupplyAnalysisEngine supplyEngine,
                                   SurgeCalculationEngine calculationEngine,
                                   PricingDecisionEngine decisionEngine,
                                   HmacTokenService tokenService,
                                   H3Core h3) {
        this.redisTemplate = redisTemplate;
        this.surgeRuleRepository = surgeRuleRepository;
        this.surgeOverrideRepository = surgeOverrideRepository;
        this.dailySurgeSummaryRepository = dailySurgeSummaryRepository;
        this.userRepository = userRepository;
        this.mongoTemplate = mongoTemplate;
        this.demandEngine = demandEngine;
        this.supplyEngine = supplyEngine;
        this.calculationEngine = calculationEngine;
        this.decisionEngine = decisionEngine;
        this.tokenService = tokenService;
        this.h3 = h3;
    }

    @Override
    public PricingQuoteResponse calculateQuote(PricingQuoteRequest request) {
        // 1. Resolve coordinates to H3 cell resolution 8
        String h3Index;
        try {
            h3Index = h3.latLngToCellAddress(request.getDeliveryLatitude(), request.getDeliveryLongitude(), 8);
        } catch (Exception e) {
            throw new IllegalArgumentException("Invalid coordinate parameters provided");
        }

        // 2. Pre-check: Global Emergency Kill Switch
        if (Boolean.TRUE.equals(redisTemplate.hasKey("surge:emergency:disabled"))) {
            return buildDefaultResponse(request, h3Index, 1.0);
        }

        // 3. Pre-check: Check active manual admin overrides
        List<SurgeOverride> overrides = surgeOverrideRepository.findByH3IndexAndIsActiveTrueAndExpiresAtAfter(h3Index, LocalDateTime.now());
        if (!overrides.isEmpty()) {
            double overrideVal = overrides.get(0).getTargetMultiplier();
            return buildDefaultResponse(request, h3Index, overrideVal);
        }

        // 4. Retrieve smoothed active surge rate (incorporates Cache Stampede protection)
        double surgeMultiplier = getActiveSurgeMultiplier(h3Index);

        // 5. Calculate base and total delivery fees
        double baseFee = 30.0;
        double finalFee = Math.round((baseFee * surgeMultiplier) * 100.0) / 100.0;

        // 6. Generate cryptographic quote token (120s TTL)
        long expiresAt = (System.currentTimeMillis() / 1000) + 120; // 120 seconds in future
        String token = tokenService.generateToken(
                request.getCartId(), // treated as client reference/idempotency key context
                request.getRestaurantId(),
                h3Index,
                surgeMultiplier,
                finalFee,
                expiresAt
        );

        // Track Checkout Attempts in telemetry
        demandEngine.registerCheckoutAttempt(h3Index);

        return new PricingQuoteResponse(finalFee, baseFee, surgeMultiplier, token, expiresAt);
    }

    @Override
    @Transactional
    public AdminSurgeOverrideResponse createOverride(AdminSurgeOverrideRequest request, String adminEmail) {
        LocalDateTime expiresAt = LocalDateTime.now().plusMinutes(request.getDurationMinutes());
        
        SurgeOverride override = new SurgeOverride(
                request.getH3Index(),
                request.getTargetMultiplier(),
                expiresAt,
                request.getReason(),
                adminEmail
        );
        override = surgeOverrideRepository.save(override);

        // Update active override cache in Redis
        String key = "surge:override:" + request.getH3Index();
        redisTemplate.opsForValue().set(key, request.getTargetMultiplier());
        redisTemplate.expire(key, java.time.Duration.ofMinutes(request.getDurationMinutes()));

        return new AdminSurgeOverrideResponse(
                override.getId(),
                override.getH3Index(),
                override.getTargetMultiplier(),
                override.getExpiresAt(),
                "ACTIVE"
        );
    }

    @Override
    @Transactional
    public void removeOverride(String overrideId) {
        SurgeOverride override = surgeOverrideRepository.findById(overrideId)
                .orElseThrow(() -> new IllegalArgumentException("Override configurations not found"));
        override.setActive(false);
        surgeOverrideRepository.save(override);

        // Evict cache key
        redisTemplate.delete("surge:override:" + override.getH3Index());
    }

    @Override
    public void setEmergencyDisable(boolean disable, String zoneName) {
        if (disable) {
            redisTemplate.opsForValue().set("surge:emergency:disabled", "TRUE");
            LOGGER.warning("EMERGENCY PRICE SURGE DISABLE INITIATED SYSTEM-WIDE");
        } else {
            redisTemplate.delete("surge:emergency:disabled");
        }
    }

    @Override
    public double getActiveSurgeMultiplier(String h3Index) {
        // Read Cache
        Double cached = getCachedMultiplier(h3Index);
        if (cached != null) {
            return cached;
        }

        // Cache Miss: Run calculation serializing via a Redisson-style mutex to prevent Cache Stampedes
        String lockKey = "lock:surge:h3:" + h3Index;
        String lockValue = UUID.randomUUID().toString(); // Fix #3: unique lock UUID
        Boolean acquired = redisTemplate.opsForValue().setIfAbsent(lockKey, lockValue, java.time.Duration.ofSeconds(5));

        if (Boolean.TRUE.equals(acquired)) {
            try {
                // Double check cache
                cached = getCachedMultiplier(h3Index);
                if (cached != null) {
                    return cached;
                }

                // Compute fresh metrics
                double demandScore = demandEngine.calculateDemandScore(h3Index);
                long pendingCount = getPendingOrdersCountForH3(h3Index);
                double supplyScore = supplyEngine.calculateSupplyScore(h3Index, pendingCount);
                long checkouts = getCheckoutsCountForH3(h3Index);
                double dpi = supplyEngine.calculateDriverPressureIndex(h3Index, pendingCount, checkouts);

                // Fetch rule configuration
                SurgeRule rule = getSurgeRuleForH3(h3Index);

                // Calculate multiplier
                double rawMultiplier = calculationEngine.calculateSurgeMultiplier(
                        h3Index, demandScore, supplyScore, dpi,
                        rule.getThresholdSurge(), rule.getScaleFactor(),
                        rule.getMaxMultiplier(), rule.getBaseMultiplier()
                );

                // Apply price damping EMA filter (Fix #2: retrieve from last calculated cache)
                double previousVal = getPreviousMultiplier(h3Index);
                double finalMultiplier = decisionEngine.getSmoothedMultiplier(h3Index, rawMultiplier, previousVal);

                // Cache the rate (TTL 60s)
                setCachedMultiplier(h3Index, finalMultiplier);
                
                // Track daily metrics history
                saveDailySurgeMetrics(h3Index, finalMultiplier);

                return finalMultiplier;
            } finally {
                // Fix #3: Safe atomic lock release via Lua Script compare-and-delete
                String script = "if redis.call('get', KEYS[1]) == ARGV[1] then return redis.call('del', KEYS[1]) else return 0 end";
                redisTemplate.execute(
                        new org.springframework.data.redis.core.script.DefaultRedisScript<>(script, Long.class),
                        java.util.Collections.singletonList(lockKey),
                        lockValue
                );
            }
        } else {
            // Wait and read from cache (spin wait)
            try {
                Thread.sleep(100);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
            cached = getCachedMultiplier(h3Index);
            return cached != null ? cached : 1.0;
        }
    }

    private Double getCachedMultiplier(String h3Index) {
        Object val = redisTemplate.opsForValue().get("surge:h3:" + h3Index);
        if (val != null) {
            try {
                return Double.parseDouble(val.toString());
            } catch (Exception e) {
                return null;
            }
        }
        return null;
    }

    private void setCachedMultiplier(String h3Index, double multiplier) {
        redisTemplate.opsForValue().set("surge:h3:" + h3Index, multiplier, java.time.Duration.ofSeconds(60));
        // Fix #2: Store last calculated multiplier with 5 minutes TTL
        redisTemplate.opsForValue().set("surge:last_calculated:" + h3Index, multiplier, java.time.Duration.ofMinutes(5));
    }

    private double getPreviousMultiplier(String h3Index) {
        Object val = redisTemplate.opsForValue().get("surge:last_calculated:" + h3Index);
        if (val != null) {
            try {
                return Double.parseDouble(val.toString());
            } catch (Exception e) {
                // Ignore parsing errors
            }
        }
        // Fallback to daily average or 1.0 if cache is cold
        return getPreviousMultiplierFromHistory(h3Index);
    }

    private double getPreviousMultiplierFromHistory(String h3Index) {
        String today = LocalDate.now().toString();
        return dailySurgeSummaryRepository.findByH3IndexAndDate(h3Index, today)
                .map(DailySurgeSummary::getAvgMultiplier)
                .orElse(1.0);
    }

    private SurgeRule getSurgeRuleForH3(String h3Index) {
        // Look up by default or find active rules (simulate fallback configuration)
        return surgeRuleRepository.findByZoneName("GLOBAL_DEFAULT")
                .orElseGet(SurgeRule::new);
    }

    private long getPendingOrdersCountForH3(String h3Index) {
        // Stub delegate
        return redisTemplate.opsForSet().size("set:drivers:h3_res8:" + h3Index) == null ? 0L : 2L; 
    }

    private long getCheckoutsCountForH3(String h3Index) {
        Long val = redisTemplate.opsForZSet().zCard("zset:checkouts:" + h3Index);
        return val != null ? val : 0L;
    }

    private PricingQuoteResponse buildDefaultResponse(PricingQuoteRequest request, String h3Index, double multiplier) {
        double baseFee = 30.0;
        double finalFee = baseFee * multiplier;
        long expiresAt = (System.currentTimeMillis() / 1000) + 120;
        String token = tokenService.generateToken(
                request.getCartId(),
                request.getRestaurantId(),
                h3Index,
                multiplier,
                finalFee,
                expiresAt
        );
        return new PricingQuoteResponse(finalFee, baseFee, multiplier, token, expiresAt);
    }

    private void saveDailySurgeMetrics(String h3Index, double multiplier) {
        String today = LocalDate.now().toString();
        
        org.springframework.data.mongodb.core.query.Query query = new org.springframework.data.mongodb.core.query.Query(
                org.springframework.data.mongodb.core.query.Criteria.where("h3Index").is(h3Index).and("date").is(today)
        );

        // Fix #5: Atomic increments and max score updates using MongoTemplate findAndModify
        org.springframework.data.mongodb.core.query.Update update = new org.springframework.data.mongodb.core.query.Update()
                .inc("surgeOrderCount", 1)
                .max("maxMultiplier", multiplier)
                .set("lastUpdatedAt", LocalDateTime.now());

        DailySurgeSummary summary = mongoTemplate.findAndModify(
                query,
                update,
                org.springframework.data.mongodb.core.FindAndModifyOptions.options().returnNew(true).upsert(true),
                DailySurgeSummary.class
        );

        if (summary != null) {
            // Atomic CAS Update for rolling average multiplier
            boolean avgUpdated = false;
            int retries = 0;
            while (!avgUpdated && retries < 5) {
                double oldAvg = summary.getAvgMultiplier();
                long count = summary.getSurgeOrderCount();
                double newAvg = count <= 1 ? multiplier : ((oldAvg * (count - 1)) + multiplier) / count;

                org.springframework.data.mongodb.core.query.Query avgQuery = new org.springframework.data.mongodb.core.query.Query(
                        org.springframework.data.mongodb.core.query.Criteria.where("id").is(summary.getId())
                                .and("avgMultiplier").is(oldAvg)
                );
                org.springframework.data.mongodb.core.query.Update avgUpdate = new org.springframework.data.mongodb.core.query.Update()
                        .set("avgMultiplier", newAvg);

                if (mongoTemplate.updateFirst(avgQuery, avgUpdate, DailySurgeSummary.class).getModifiedCount() > 0) {
                    avgUpdated = true;
                } else {
                    summary = mongoTemplate.findOne(query, DailySurgeSummary.class);
                    if (summary == null) break;
                    retries++;
                }
            }
        }
    }
}
