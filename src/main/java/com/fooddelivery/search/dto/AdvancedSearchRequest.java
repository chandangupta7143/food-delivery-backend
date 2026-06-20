package com.fooddelivery.search.dto;

import com.fooddelivery.search.enums.SearchMode;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
import java.util.List;

public class AdvancedSearchRequest {

    @NotNull(message = "Longitude is required")
    @Min(value = -180, message = "Longitude must be between -180 and 180")
    @Max(value = 180, message = "Longitude must be between -180 and 180")
    private Double longitude;

    @NotNull(message = "Latitude is required")
    @Min(value = -90, message = "Latitude must be between -90 and 90")
    @Max(value = 90, message = "Latitude must be between -90 and 90")
    private Double latitude;

    @NotNull(message = "Search mode is required")
    private SearchMode searchMode;

    @Size(max = 100, message = "Query length must not exceed 100 characters")
    private String query;

    @Min(value = 1, message = "Radius must be at least 1 km")
    @Max(value = 50, message = "Radius must be at most 50 km")
    private Double radiusInKm = 5.0;

    @Size(max = 10, message = "Cannot select more than 10 cuisines")
    private List<String> cuisines;

    @Min(value = 0, message = "Min rating must be at least 0")
    @Max(value = 5, message = "Min rating must be at most 5")
    private Double minRating;

    @Positive(message = "Max delivery time must be positive")
    private Integer maxDeliveryTime;

    @Size(max = 4, message = "Cannot select more than 4 price ranges")
    private List<Integer> priceRanges;

    private Boolean isVegetarian;

    private String sortBy; // "DISTANCE", "RATING", "POPULARITY", "DELIVERY_TIME", "RELEVANCE"

    @Min(value = 0, message = "Page must be at least 0")
    private int page = 0;

    @Min(value = 1, message = "Size must be at least 1")
    @Max(value = 50, message = "Size must be at most 50")
    private int size = 20;

    // Getters and Setters
    public Double getLongitude() { return longitude; }
    public void setLongitude(Double longitude) { this.longitude = longitude; }

    public Double getLatitude() { return latitude; }
    public void setLatitude(Double latitude) { this.latitude = latitude; }

    public SearchMode getSearchMode() { return searchMode; }
    public void setSearchMode(SearchMode searchMode) { this.searchMode = searchMode; }

    public String getQuery() { return query; }
    public void setQuery(String query) { this.query = query; }

    public Double getRadiusInKm() { return radiusInKm; }
    public void setRadiusInKm(Double radiusInKm) { this.radiusInKm = radiusInKm; }

    public List<String> getCuisines() { return cuisines; }
    public void setCuisines(List<String> cuisines) { this.cuisines = cuisines; }

    public Double getMinRating() { return minRating; }
    public void setMinRating(Double minRating) { this.minRating = minRating; }

    public Integer getMaxDeliveryTime() { return maxDeliveryTime; }
    public void setMaxDeliveryTime(Integer maxDeliveryTime) { this.maxDeliveryTime = maxDeliveryTime; }

    public List<Integer> getPriceRanges() { return priceRanges; }
    public void setPriceRanges(List<Integer> priceRanges) { this.priceRanges = priceRanges; }

    public Boolean getIsVegetarian() { return isVegetarian; }
    public void setIsVegetarian(Boolean isVegetarian) { this.isVegetarian = isVegetarian; }

    public String getSortBy() { return sortBy; }
    public void setSortBy(String sortBy) { this.sortBy = sortBy; }

    public int getPage() { return page; }
    public void setPage(int page) { this.page = page; }

    public int getSize() { return size; }
    public void setSize(int size) { this.size = size; }
}
