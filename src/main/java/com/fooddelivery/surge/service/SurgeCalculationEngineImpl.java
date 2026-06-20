package com.fooddelivery.surge.service;

import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.util.logging.Logger;

@Service
public class SurgeCalculationEngineImpl implements SurgeCalculationEngine {

    private static final Logger LOGGER = Logger.getLogger(SurgeCalculationEngineImpl.class.getName());

    private final RedisTemplate<String, Object> redisTemplate;

    public SurgeCalculationEngineImpl(RedisTemplate<String, Object> redisTemplate) {
        this.redisTemplate = redisTemplate;
    }

    @Override
    public double calculateSurgeMultiplier(String h3Index, double demandScore, double supplyScore, double dpi,
                                    double threshold, double scaleFactor, double maxCap, double baseMultiplier) {
        // 1. Calculate pressure score
        double rawPressure = (0.40 * demandScore) +
                              (0.40 * (100.0 - supplyScore)) +
                              (0.20 * Math.min(100.0, dpi * 20.0));

        // 2. Fetch cached weather and traffic factors (O(1) cache reads)
        double weatherFactor = getCachedFactor("surge:weather:" + h3Index);
        double trafficFactor = getCachedFactor("surge:traffic:" + h3Index);
        double eventFactor = getCachedFactor("surge:event:" + h3Index);

        // 3. Compute multiplier
        double excessPressure = Math.max(0.0, rawPressure - threshold);
        double multiplier = baseMultiplier + (excessPressure / scaleFactor) * weatherFactor * trafficFactor * eventFactor;

        // Apply Hard Cap
        return Math.min(maxCap, Math.max(baseMultiplier, multiplier));
    }

    private double getCachedFactor(String key) {
        Object val = redisTemplate.opsForValue().get(key);
        if (val == null) {
            return 1.0; // default multiplier
        }
        try {
            if (val instanceof Number) {
                return ((Number) val).doubleValue();
            }
            return Double.parseDouble(val.toString());
        } catch (Exception e) {
            return 1.0;
        }
    }
}
