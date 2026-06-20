package com.fooddelivery.recommendations.service;

import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;

import java.util.logging.Logger;

@Component
public class RecommendationTelemetryProducer {

    private static final Logger LOGGER = Logger.getLogger(RecommendationTelemetryProducer.class.getName());

    /**
     * Publishes interaction telemetry asynchronously to simulate Kafka payload ingestion.
     */
    @Async
    public void publishUserInteraction(String userId, String restaurantId, String eventType, String h3Index) {
        String eventPayload = String.format(
                "{\"topic\": \"user-interactions-telemetry\", \"userId\": \"%s\", \"restaurantId\": \"%s\", \"eventType\": \"%s\", \"h3Index\": \"%s\", \"timestamp\": %d}",
                userId, restaurantId, eventType, h3Index, System.currentTimeMillis() / 1000
        );
        LOGGER.info("Kafka Telemetry Emitted: " + eventPayload);
    }

    /**
     * Publishes operational status updates of restaurants asynchronously to Kafka.
     */
    @Async
    public void publishRestaurantOperationalUpdate(String restaurantId, String h3Index, String status, int prepTime, int couriers) {
        String eventPayload = String.format(
                "{\"topic\": \"restaurant-operational-updates\", \"restaurantId\": \"%s\", \"h3Index\": \"%s\", \"operationalStatus\": \"%s\", \"averagePrepTime\": %d, \"courierCapacity\": %d, \"timestamp\": %d}",
                restaurantId, h3Index, status, prepTime, couriers, System.currentTimeMillis() / 1000
        );
        LOGGER.info("Kafka Operational Update Emitted: " + eventPayload);
    }
}
