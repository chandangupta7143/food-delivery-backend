package com.fooddelivery.restaurants.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

public class NearbySearchRequest {

    @NotNull(message = "Longitude is required")
    @Min(value = -180, message = "Longitude must be between -180 and 180")
    @Max(value = 180, message = "Longitude must be between -180 and 180")
    private Double longitude;

    @NotNull(message = "Latitude is required")
    @Min(value = -90, message = "Latitude must be between -90 and 90")
    @Max(value = 90, message = "Latitude must be between -90 and 90")
    private Double latitude;

    @Min(value = 1, message = "Radius must be at least 1 km")
    @Max(value = 50, message = "Radius must be at most 50 km")
    private Double radiusKm = 5.0; // Default 5km

    private String cuisine;
    
    @Min(value = 0, message = "Min rating must be at least 0")
    @Max(value = 5, message = "Min rating must be at most 5")
    private Double minRating;
    
    @Min(value = 1, message = "Price range must be at least 1")
    @Max(value = 4, message = "Price range must be at most 4")
    private Integer priceRange;
    
    private Boolean vegetarianOnly;
    
    @Positive(message = "Max delivery time must be positive")
    private Integer maxDeliveryTime;

    private String sortBy; // "distance", "rating", "popularity"

    @Min(value = 0, message = "Page must be at least 0")
    private int page = 0;
    
    @Min(value = 1, message = "Size must be at least 1")
    @Max(value = 50, message = "Size must be at most 50")
    private int size = 20;

    public Double getLongitude() { return longitude; }
    public void setLongitude(Double longitude) { this.longitude = longitude; }

    public Double getLatitude() { return latitude; }
    public void setLatitude(Double latitude) { this.latitude = latitude; }

    public Double getRadiusKm() { return radiusKm; }
    public void setRadiusKm(Double radiusKm) { this.radiusKm = radiusKm; }

    public String getCuisine() { return cuisine; }
    public void setCuisine(String cuisine) { this.cuisine = cuisine; }

    public Double getMinRating() { return minRating; }
    public void setMinRating(Double minRating) { this.minRating = minRating; }

    public Integer getPriceRange() { return priceRange; }
    public void setPriceRange(Integer priceRange) { this.priceRange = priceRange; }

    public Boolean getVegetarianOnly() { return vegetarianOnly; }
    public void setVegetarianOnly(Boolean vegetarianOnly) { this.vegetarianOnly = vegetarianOnly; }

    public Integer getMaxDeliveryTime() { return maxDeliveryTime; }
    public void setMaxDeliveryTime(Integer maxDeliveryTime) { this.maxDeliveryTime = maxDeliveryTime; }

    public String getSortBy() { return sortBy; }
    public void setSortBy(String sortBy) { this.sortBy = sortBy; }

    public int getPage() { return page; }
    public void setPage(int page) { this.page = page; }

    public int getSize() { return size; }
    public void setSize(int size) { this.size = size; }
}
