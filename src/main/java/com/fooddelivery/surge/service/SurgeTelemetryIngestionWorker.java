package com.fooddelivery.surge.service;

import com.fooddelivery.surge.entity.SurgeRule;
import com.fooddelivery.surge.repository.SurgeRuleRepository;
import com.uber.h3core.H3Core;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Random;
import java.util.logging.Logger;

@Service
public class SurgeTelemetryIngestionWorker {

    private static final Logger LOGGER = Logger.getLogger(SurgeTelemetryIngestionWorker.class.getName());
    private static final Random RANDOM = new Random();

    private final RedisTemplate<String, Object> redisTemplate;
    private final SurgeRuleRepository surgeRuleRepository;
    private final H3Core h3;

    public SurgeTelemetryIngestionWorker(RedisTemplate<String, Object> redisTemplate,
                                         SurgeRuleRepository surgeRuleRepository,
                                         H3Core h3) {
        this.redisTemplate = redisTemplate;
        this.surgeRuleRepository = surgeRuleRepository;
        this.h3 = h3;
    }

    // Runs every 10 minutes (600,000 milliseconds)
    @Scheduled(fixedRate = 600000, initialDelay = 10000)
    public void ingestEnvironmentalSignals() {
        LOGGER.info("Starting background weather and traffic signal ingestion worker");

        List<SurgeRule> activeRules = surgeRuleRepository.findAll();
        if (activeRules.isEmpty()) {
            // Build a default rule if none exist
            SurgeRule defaultRule = new SurgeRule();
            defaultRule.setZoneName("GLOBAL_DEFAULT");
            activeRules = List.of(defaultRule);
        }

        for (SurgeRule rule : activeRules) {
            String zone = rule.getZoneName();
            double lat = 28.6282; // Default to Noida
            double lng = 77.3898;

            if ("Delhi_NCR".equalsIgnoreCase(zone)) {
                lat = 28.6139;
                lng = 77.2090;
            } else if ("Gurgaon".equalsIgnoreCase(zone)) {
                lat = 28.4595;
                lng = 77.0266;
            } else if ("Mumbai".equalsIgnoreCase(zone)) {
                lat = 19.0760;
                lng = 72.8777;
            } else if ("Bangalore".equalsIgnoreCase(zone)) {
                lat = 12.9716;
                lng = 77.5946;
            }

            try {
                // Resolve zone coordinates to H3 Resolution 8 index
                String baseH3 = h3.latLngToCellAddress(lat, lng, 8);
                
                // 1. Simulate and cache weather factor (e.g. Rain = 1.25, Snow = 1.50)
                double weatherFactor = simulateWeatherFactor();
                redisTemplate.opsForValue().set("surge:weather:" + baseH3, weatherFactor, java.time.Duration.ofMinutes(15));
                LOGGER.info("Cached weather factor [" + weatherFactor + "] for Zone " + zone + " (H3: " + baseH3 + ")");

                // 2. Simulate and cache traffic factor (e.g. Congestion = 1.15)
                double trafficFactor = simulateTrafficFactor();
                redisTemplate.opsForValue().set("surge:traffic:" + baseH3, trafficFactor, java.time.Duration.ofMinutes(15));
                LOGGER.info("Cached traffic factor [" + trafficFactor + "] for Zone " + zone + " (H3: " + baseH3 + ")");

                // 3. Simulate and cache special event factor (e.g. Stadium exit = 1.30)
                double eventFactor = 1.0; // standard default
                redisTemplate.opsForValue().set("surge:event:" + baseH3, eventFactor, java.time.Duration.ofMinutes(15));

            } catch (Exception e) {
                LOGGER.severe("Background telemetry signal ingestion failed for Zone " + zone + ": " + e.getMessage());
            }
        }
    }

    private double simulateWeatherFactor() {
        // 10% chance of rain (1.25 multiplier), 90% clear (1.00 multiplier)
        int choice = RANDOM.nextInt(10);
        if (choice == 1) {
            return 1.25;
        } else if (choice == 2) {
            return 1.50; // heavy rain/snow storm
        }
        return 1.0;
    }

    private double simulateTrafficFactor() {
        // 20% chance of rush-hour congestion delays
        int choice = RANDOM.nextInt(5);
        if (choice == 1) {
            return 1.15;
        } else if (choice == 2) {
            return 1.30; // heavy congestion gridlock
        }
        return 1.0;
    }
}
