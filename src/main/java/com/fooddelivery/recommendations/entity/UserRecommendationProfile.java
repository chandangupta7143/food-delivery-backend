package com.fooddelivery.recommendations.entity;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Document(collection = "user_recommendation_profiles")
public class UserRecommendationProfile {

    @Id
    private String id;

    @Indexed(unique = true)
    private String userId;

    @org.springframework.data.annotation.Version
    private Long version;

    private List<CuisineAffinity> cuisineAffinity = new ArrayList<>();
    private List<RestaurantAffinity> restaurantAffinities = new ArrayList<>();
    
    private PricePreference pricePreference = new PricePreference();
    private String vegNonVegPreference = "MIXED"; // VEG_ONLY, MIXED
    private TimeOfDayPreference timeOfDayPreference = new TimeOfDayPreference();
    private LocalDateTime updatedAt = LocalDateTime.now();

    public UserRecommendationProfile() {
    }

    public UserRecommendationProfile(String userId) {
        this.userId = userId;
    }

    // Nested Classes for Sub-Documents

    public static class CuisineAffinity {
        private String cuisine;
        private double score;
        private LocalDateTime lastUpdatedAt;

        public CuisineAffinity() {
        }

        public CuisineAffinity(String cuisine, double score, LocalDateTime lastUpdatedAt) {
            this.cuisine = cuisine;
            this.score = score;
            this.lastUpdatedAt = lastUpdatedAt;
        }

        public String getCuisine() { return cuisine; }
        public void setCuisine(String cuisine) { this.cuisine = cuisine; }

        public double getScore() { return score; }
        public void setScore(double score) { this.score = score; }

        public LocalDateTime getLastUpdatedAt() { return lastUpdatedAt; }
        public void setLastUpdatedAt(LocalDateTime lastUpdatedAt) { this.lastUpdatedAt = lastUpdatedAt; }
    }

    public static class RestaurantAffinity {
        private String restaurantId;
        private long orderCount;
        private double affinityScore;
        private LocalDateTime lastOrderedAt;

        public RestaurantAffinity() {
        }

        public RestaurantAffinity(String restaurantId, long orderCount, double affinityScore, LocalDateTime lastOrderedAt) {
            this.restaurantId = restaurantId;
            this.orderCount = orderCount;
            this.affinityScore = affinityScore;
            this.lastOrderedAt = lastOrderedAt;
        }

        public String getRestaurantId() { return restaurantId; }
        public void setRestaurantId(String restaurantId) { this.restaurantId = restaurantId; }

        public long getOrderCount() { return orderCount; }
        public void setOrderCount(long orderCount) { this.orderCount = orderCount; }

        public double getAffinityScore() { return affinityScore; }
        public void setAffinityScore(double affinityScore) { this.affinityScore = affinityScore; }

        public LocalDateTime getLastOrderedAt() { return lastOrderedAt; }
        public void setLastOrderedAt(LocalDateTime lastOrderedAt) { this.lastOrderedAt = lastOrderedAt; }
    }

    public static class PricePreference {
        private double tier1Ratio = 0.25;
        private double tier2Ratio = 0.25;
        private double tier3Ratio = 0.25;
        private double tier4Ratio = 0.25;

        public PricePreference() {}

        public double getTier1Ratio() { return tier1Ratio; }
        public void setTier1Ratio(double tier1Ratio) { this.tier1Ratio = tier1Ratio; }

        public double getTier2Ratio() { return tier2Ratio; }
        public void setTier2Ratio(double tier2Ratio) { this.tier2Ratio = tier2Ratio; }

        public double getTier3Ratio() { return tier3Ratio; }
        public void setTier3Ratio(double tier3Ratio) { this.tier3Ratio = tier3Ratio; }

        public double getTier4Ratio() { return tier4Ratio; }
        public void setTier4Ratio(double tier4Ratio) { this.tier4Ratio = tier4Ratio; }
    }

    public static class TimeOfDayPreference {
        private double breakfast = 0.25;
        private double lunch = 0.25;
        private double dinner = 0.25;
        private double lateNight = 0.25;

        public TimeOfDayPreference() {}

        public double getBreakfast() { return breakfast; }
        public void setBreakfast(double breakfast) { this.breakfast = breakfast; }

        public double getLunch() { return lunch; }
        public void setLunch(double lunch) { this.lunch = lunch; }

        public double getDinner() { return dinner; }
        public void setDinner(double dinner) { this.dinner = dinner; }

        public double getLateNight() { return lateNight; }
        public void setLateNight(double lateNight) { this.lateNight = lateNight; }
    }

    // Getters and Setters

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public String getUserId() { return userId; }
    public void setUserId(String userId) { this.userId = userId; }

    public List<CuisineAffinity> getCuisineAffinity() { return cuisineAffinity; }
    public void setCuisineAffinity(List<CuisineAffinity> cuisineAffinity) { this.cuisineAffinity = cuisineAffinity; }

    public List<RestaurantAffinity> getRestaurantAffinities() { return restaurantAffinities; }
    public void setRestaurantAffinities(List<RestaurantAffinity> restaurantAffinities) { this.restaurantAffinities = restaurantAffinities; }

    public PricePreference getPricePreference() { return pricePreference; }
    public void setPricePreference(PricePreference pricePreference) { this.pricePreference = pricePreference; }

    public String getVegNonVegPreference() { return vegNonVegPreference; }
    public void setVegNonVegPreference(String vegNonVegPreference) { this.vegNonVegPreference = vegNonVegPreference; }

    public TimeOfDayPreference getTimeOfDayPreference() { return timeOfDayPreference; }
    public void setTimeOfDayPreference(TimeOfDayPreference timeOfDayPreference) { this.timeOfDayPreference = timeOfDayPreference; }

    public LocalDateTime getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(LocalDateTime updatedAt) { this.updatedAt = updatedAt; }

    public Long getVersion() { return version; }
    public void setVersion(Long version) { this.version = version; }
}
