package com.fooddelivery.recommendations.entity;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Document(collection = "recommendation_events")
public class RecommendationEvent {

    @Id
    private String id;

    @Indexed(unique = true)
    private String eventId;

    @Indexed
    private String userId;

    @org.springframework.data.annotation.Version
    private Long version;

    private String h3Index;
    private String modelVersion;
    private String rankingVersion;
    
    private List<RenderedRestaurant> renderedRestaurants = new ArrayList<>();
    private List<ClickEvent> clicks = new ArrayList<>();
    private List<ConversionEvent> conversions = new ArrayList<>();
    private LocalDateTime timestamp = LocalDateTime.now();

    public RecommendationEvent() {
    }

    public RecommendationEvent(String eventId, String userId, String h3Index, String modelVersion, String rankingVersion) {
        this.eventId = eventId;
        this.userId = userId;
        this.h3Index = h3Index;
        this.modelVersion = modelVersion;
        this.rankingVersion = rankingVersion;
    }

    // Nested Classes for Sub-Documents

    public static class RenderedRestaurant {
        private String restaurantId;
        private int position;
        private double score;

        public RenderedRestaurant() {
        }

        public RenderedRestaurant(String restaurantId, int position, double score) {
            this.restaurantId = restaurantId;
            this.position = position;
            this.score = score;
        }

        public String getRestaurantId() { return restaurantId; }
        public void setRestaurantId(String restaurantId) { this.restaurantId = restaurantId; }

        public int getPosition() { return position; }
        public void setPosition(int position) { this.position = position; }

        public double getScore() { return score; }
        public void setScore(double score) { this.score = score; }
    }

    public static class ClickEvent {
        private String restaurantId;
        private LocalDateTime clickedAt;

        public ClickEvent() {
        }

        public ClickEvent(String restaurantId, LocalDateTime clickedAt) {
            this.restaurantId = restaurantId;
            this.clickedAt = clickedAt;
        }

        public String getRestaurantId() { return restaurantId; }
        public void setRestaurantId(String restaurantId) { this.restaurantId = restaurantId; }

        public LocalDateTime getClickedAt() { return clickedAt; }
        public void setClickedAt(LocalDateTime clickedAt) { this.clickedAt = clickedAt; }
    }

    public static class ConversionEvent {
        private String restaurantId;
        private String orderId;
        private LocalDateTime orderedAt;
        private double revenue;

        public ConversionEvent() {
        }

        public ConversionEvent(String restaurantId, String orderId, LocalDateTime orderedAt, double revenue) {
            this.restaurantId = restaurantId;
            this.orderId = orderId;
            this.orderedAt = orderedAt;
            this.revenue = revenue;
        }

        public String getRestaurantId() { return restaurantId; }
        public void setRestaurantId(String restaurantId) { this.restaurantId = restaurantId; }

        public String getOrderId() { return orderId; }
        public void setOrderId(String orderId) { this.orderId = orderId; }

        public LocalDateTime getOrderedAt() { return orderedAt; }
        public void setOrderedAt(LocalDateTime orderedAt) { this.orderedAt = orderedAt; }

        public double getRevenue() { return revenue; }
        public void setRevenue(double revenue) { this.revenue = revenue; }
    }

    // Getters and Setters

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public String getEventId() { return eventId; }
    public void setEventId(String eventId) { this.eventId = eventId; }

    public String getUserId() { return userId; }
    public void setUserId(String userId) { this.userId = userId; }

    public String getH3Index() { return h3Index; }
    public void setH3Index(String h3Index) { this.h3Index = h3Index; }

    public String getModelVersion() { return modelVersion; }
    public void setModelVersion(String modelVersion) { this.modelVersion = modelVersion; }

    public String getRankingVersion() { return rankingVersion; }
    public void setRankingVersion(String rankingVersion) { this.rankingVersion = rankingVersion; }

    public List<RenderedRestaurant> getRenderedRestaurants() { return renderedRestaurants; }
    public void setRenderedRestaurants(List<RenderedRestaurant> renderedRestaurants) { this.renderedRestaurants = renderedRestaurants; }

    public List<ClickEvent> getClicks() { return clicks; }
    public void setClicks(List<ClickEvent> clicks) { this.clicks = clicks; }

    public List<ConversionEvent> getConversions() { return conversions; }
    public void setConversions(List<ConversionEvent> conversions) { this.conversions = conversions; }

    public LocalDateTime getTimestamp() { return timestamp; }
    public void setTimestamp(LocalDateTime timestamp) { this.timestamp = timestamp; }

    public Long getVersion() { return version; }
    public void setVersion(Long version) { this.version = version; }
}
