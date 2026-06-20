package com.fooddelivery.search.dto;

import com.fooddelivery.restaurants.dto.OperatingHourResponse;
import java.util.List;

public class AdvancedSearchResponse {

    private String id;
    private String name;
    private List<String> images; // Only first image will be populated in mapping

    private List<String> cuisines;
    private int priceRange;
    private boolean isVegetarian;

    // Recommendation Engine Compatibility Fields
    private double averageRating;
    private int totalRatings;
    private double popularityScore;

    private int averageDeliveryTimeMinutes;

    private double longitude;
    private double latitude;
    private double distanceInKm; // Computed if DISCOVERY mode

    private List<OperatingHourResponse> operatingHours;
    private boolean openNow; // Recalculated dynamically

    // Getters and Setters
    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public List<String> getImages() { return images; }
    public void setImages(List<String> images) { this.images = images; }

    public List<String> getCuisines() { return cuisines; }
    public void setCuisines(List<String> cuisines) { this.cuisines = cuisines; }

    public int getPriceRange() { return priceRange; }
    public void setPriceRange(int priceRange) { this.priceRange = priceRange; }

    public boolean isVegetarian() { return isVegetarian; }
    public void setVegetarian(boolean vegetarian) { isVegetarian = vegetarian; }

    public double getAverageRating() { return averageRating; }
    public void setAverageRating(double averageRating) { this.averageRating = averageRating; }

    public int getTotalRatings() { return totalRatings; }
    public void setTotalRatings(int totalRatings) { this.totalRatings = totalRatings; }

    public double getPopularityScore() { return popularityScore; }
    public void setPopularityScore(double popularityScore) { this.popularityScore = popularityScore; }

    public int getAverageDeliveryTimeMinutes() { return averageDeliveryTimeMinutes; }
    public void setAverageDeliveryTimeMinutes(int averageDeliveryTimeMinutes) { this.averageDeliveryTimeMinutes = averageDeliveryTimeMinutes; }

    public double getLongitude() { return longitude; }
    public void setLongitude(double longitude) { this.longitude = longitude; }

    public double getLatitude() { return latitude; }
    public void setLatitude(double latitude) { this.latitude = latitude; }

    public double getDistanceInKm() { return distanceInKm; }
    public void setDistanceInKm(double distanceInKm) { this.distanceInKm = distanceInKm; }

    public List<OperatingHourResponse> getOperatingHours() { return operatingHours; }
    public void setOperatingHours(List<OperatingHourResponse> operatingHours) { this.operatingHours = operatingHours; }

    public boolean isOpenNow() { return openNow; }
    public void setOpenNow(boolean openNow) { this.openNow = openNow; }
}
