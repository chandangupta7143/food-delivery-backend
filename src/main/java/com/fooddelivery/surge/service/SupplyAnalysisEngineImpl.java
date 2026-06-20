package com.fooddelivery.surge.service;

import com.uber.h3core.H3Core;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.util.logging.Logger;

@Service
public class SupplyAnalysisEngineImpl implements SupplyAnalysisEngine {

    private static final Logger LOGGER = Logger.getLogger(SupplyAnalysisEngineImpl.class.getName());

    private final RedisTemplate<String, Object> redisTemplate;
    private final H3Core h3;

    public SupplyAnalysisEngineImpl(RedisTemplate<String, Object> redisTemplate, H3Core h3) {
        this.redisTemplate = redisTemplate;
        this.h3 = h3;
    }

    @Override
    public void registerDriverLocation(String driverId, double lat, double lng, String status) {
        try {
            // Resolve H3 indexes at Resolutions 7 and 8
            String h3Res8 = h3.latLngToCellAddress(lat, lng, 8);
            String h3Res7 = h3.latLngToCellAddress(lat, lng, 7);

            // Fetch previous H3 mappings for driver state cleanup
            String stateKey = "driver:state:" + driverId;
            String previousState = (String) redisTemplate.opsForValue().get(stateKey);

            if (previousState != null) {
                String[] prevHex = previousState.split(":");
                if (prevHex.length == 2) {
                    String oldRes7 = prevHex[0];
                    String oldRes8 = prevHex[1];
                    // Clean up from previous sets
                    redisTemplate.opsForSet().remove("set:drivers:h3_res7:" + oldRes7, driverId);
                    redisTemplate.opsForSet().remove("set:drivers:h3_res8:" + oldRes8, driverId);
                }
            }

            // Optional visualization index updates (secondary GEO Set)
            redisTemplate.opsForGeo().add("couriers:active", new org.springframework.data.geo.Point(lng, lat), driverId);

            // Primary Supply Source of Truth: Add to current H3 sets only if state is AVAILABLE
            if ("AVAILABLE".equalsIgnoreCase(status)) {
                redisTemplate.opsForSet().add("set:drivers:h3_res7:" + h3Res7, driverId);
                redisTemplate.opsForSet().add("set:drivers:h3_res8:" + h3Res8, driverId);
                redisTemplate.opsForValue().set(stateKey, h3Res7 + ":" + h3Res8);
                redisTemplate.expire(stateKey, java.time.Duration.ofMinutes(10)); // idle state cleanup
            } else {
                // If busy or offline, remove state tracking key
                redisTemplate.delete(stateKey);
            }

            // Expose a driver presence TTL key for fallback checkups
            String presenceKey = "driver:presence:" + driverId;
            redisTemplate.opsForValue().set(presenceKey, "ONLINE");
            redisTemplate.expire(presenceKey, java.time.Duration.ofSeconds(90));

        } catch (Exception e) {
            LOGGER.severe("Failed to register driver coordinates: " + e.getMessage());
        }
    }

    @Override
    public long getAvailableDriversCount(String h3Index) {
        long count = getActiveDriversInCell(h3Index);
        // Optional Hardening C: H3 gridDisk ring expansion
        // If immediate supply is constrained (count < 3), expand to k=1 adjacent hexagons to aggregate supply
        if (count < 3) {
            try {
                java.util.List<String> neighbors = h3.gridDisk(h3Index, 1);
                long neighborCount = 0;
                for (String neighbor : neighbors) {
                    if (!neighbor.equalsIgnoreCase(h3Index)) {
                        neighborCount += getActiveDriversInCell(neighbor);
                    }
                }
                count += neighborCount;
            } catch (Exception e) {
                // Fallback to local cell only
            }
        }
        return count;
    }

    private long getActiveDriversInCell(String cellIndex) {
        String setKey = "set:drivers:h3_res8:" + cellIndex;
        java.util.Set<Object> members = redisTemplate.opsForSet().members(setKey);
        if (members == null || members.isEmpty()) {
            return 0L;
        }
        long activeCount = 0;
        for (Object member : members) {
            String driverId = member.toString();
            // Verify presence via active TTL key to prevent silent offline driver leakage (Fix #1)
            if (Boolean.TRUE.equals(redisTemplate.hasKey("driver:presence:" + driverId))) {
                activeCount++;
            } else {
                // Prune stale membership from both Resolution 7 and 8 sets
                redisTemplate.opsForSet().remove(setKey, driverId);
                String stateKey = "driver:state:" + driverId;
                String stateVal = (String) redisTemplate.opsForValue().get(stateKey);
                if (stateVal != null) {
                    String[] hexes = stateVal.split(":");
                    if (hexes.length == 2) {
                        redisTemplate.opsForSet().remove("set:drivers:h3_res7:" + hexes[0], driverId);
                    }
                }
                redisTemplate.delete(stateKey);
            }
        }
        return activeCount;
    }

    @Override
    public double calculateSupplyScore(String h3Index, long pendingOrdersCount) {
        long available = getAvailableDriversCount(h3Index);
        
        // Supply Score formula: Measure capacity compared to load
        // Ss = max(0, 100.0 - (pendingOrders / max(1, available)) * 100)
        double ratio = (double) pendingOrdersCount / Math.max(1, available);
        double score = 100.0 - (ratio * 100.0);
        return Math.max(0.0, Math.min(100.0, score));
    }

    @Override
    public double calculateDriverPressureIndex(String h3Index, long pendingOrdersCount, long checkoutAttempts) {
        long available = getAvailableDriversCount(h3Index);
        
        // DPI = (pendingOrders + checkoutAttempts * 0.25) / max(1, available)
        double pressure = (pendingOrdersCount + (checkoutAttempts * 0.25)) / Math.max(1, available);
        return Math.max(0.0, pressure);
    }
}
