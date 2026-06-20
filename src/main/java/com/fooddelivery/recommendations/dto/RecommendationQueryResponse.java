package com.fooddelivery.recommendations.dto;

import java.util.ArrayList;
import java.util.List;

public class RecommendationQueryResponse {

    private List<RecommendationItem> recommendations = new ArrayList<>();
    private int page;
    private int size;
    private int totalPages;
    private String eventId; // UUID of the recommendation event for attribution tracking

    public RecommendationQueryResponse() {
    }

    public RecommendationQueryResponse(List<RecommendationItem> recommendations, int page, int size, int totalPages, String eventId) {
        this.recommendations = recommendations;
        this.page = page;
        this.size = size;
        this.totalPages = totalPages;
        this.eventId = eventId;
    }

    public static class RecommendationItem {
        private String restaurantId;
        private String name;
        private List<String> cuisines = new ArrayList<>();
        private int priceTier;
        private double distance;
        private int etaMinutes;
        private double surgeMultiplier;
        private double score;
        private String explanation;

        public RecommendationItem() {
        }

        public RecommendationItem(String restaurantId, String name, List<String> cuisines, int priceTier,
                                    double distance, int etaMinutes, double surgeMultiplier, double score, String explanation) {
            this.restaurantId = restaurantId;
            this.name = name;
            this.cuisines = cuisines;
            this.priceTier = priceTier;
            this.distance = distance;
            this.etaMinutes = etaMinutes;
            this.surgeMultiplier = surgeMultiplier;
            this.score = score;
            this.explanation = explanation;
        }

        // Getters and Setters
        public String getRestaurantId() { return restaurantId; }
        public void setRestaurantId(String restaurantId) { this.restaurantId = restaurantId; }

        public String getName() { return name; }
        public void setName(String name) { this.name = name; }

        public List<String> getCuisines() { return cuisines; }
        public void setCuisines(List<String> cuisines) { this.cuisines = cuisines; }

        public int getPriceTier() { return priceTier; }
        public void setPriceTier(int priceTier) { this.priceTier = priceTier; }

        public double getDistance() { return distance; }
        public void setDistance(double distance) { this.distance = distance; }

        public int getEtaMinutes() { return etaMinutes; }
        public void setEtaMinutes(int etaMinutes) { this.etaMinutes = etaMinutes; }

        public double getSurgeMultiplier() { return surgeMultiplier; }
        public void setSurgeMultiplier(double surgeMultiplier) { this.surgeMultiplier = surgeMultiplier; }

        public double getScore() { return score; }
        public void setScore(double score) { this.score = score; }

        public String getExplanation() { return explanation; }
        public void setExplanation(String explanation) { this.explanation = explanation; }
    }

    // Getters and Setters

    public List<RecommendationItem> getRecommendations() { return recommendations; }
    public void setRecommendations(List<RecommendationItem> recommendations) { this.recommendations = recommendations; }

    public int getPage() { return page; }
    public void setPage(int page) { this.page = page; }

    public int getSize() { return size; }
    public void setSize(int size) { this.size = size; }

    public int getTotalPages() { return totalPages; }
    public void setTotalPages(int totalPages) { this.totalPages = totalPages; }

    public String getEventId() { return eventId; }
    public void setEventId(String eventId) { this.eventId = eventId; }
}
