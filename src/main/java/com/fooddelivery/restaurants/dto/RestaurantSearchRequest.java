package com.fooddelivery.restaurants.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Positive;

public class RestaurantSearchRequest {

    private String cuisine;
    
    private String cityCode;
    
    @Min(value = 0, message = "Min rating must be at least 0")
    @Max(value = 5, message = "Min rating must be at most 5")
    private Double minRating;
    
    @Positive(message = "Max delivery time must be positive")
    private Integer maxDeliveryTime;
    
    @Min(value = 1, message = "Price range must be at least 1")
    @Max(value = 4, message = "Price range must be at most 4")
    private Integer priceRange;
    
    private Boolean vegetarianOnly;
    private String sortBy; // "rating", "popularity", "deliveryTime"
    
    @Min(value = 0, message = "Page must be at least 0")
    private int page = 0;
    
    @Min(value = 1, message = "Size must be at least 1")
    @Max(value = 50, message = "Size must be at most 50")
    private int size = 20;

    public String getCuisine() { return cuisine; }
    public void setCuisine(String cuisine) { this.cuisine = cuisine; }

    public String getCityCode() { return cityCode; }
    public void setCityCode(String cityCode) { this.cityCode = cityCode; }

    public Double getMinRating() { return minRating; }
    public void setMinRating(Double minRating) { this.minRating = minRating; }

    public Integer getMaxDeliveryTime() { return maxDeliveryTime; }
    public void setMaxDeliveryTime(Integer maxDeliveryTime) { this.maxDeliveryTime = maxDeliveryTime; }

    public Integer getPriceRange() { return priceRange; }
    public void setPriceRange(Integer priceRange) { this.priceRange = priceRange; }

    public Boolean getVegetarianOnly() { return vegetarianOnly; }
    public void setVegetarianOnly(Boolean vegetarianOnly) { this.vegetarianOnly = vegetarianOnly; }

    public String getSortBy() { return sortBy; }
    public void setSortBy(String sortBy) { this.sortBy = sortBy; }

    public int getPage() { return page; }
    public void setPage(int page) { this.page = page; }

    public int getSize() { return size; }
    public void setSize(int size) { this.size = size; }
}
