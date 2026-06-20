package com.fooddelivery.restaurants.dto;

import java.time.LocalDateTime;
import java.util.List;

public class RestaurantResponse {

    private String id;
    private String name;
    private String description;
    private String contactPersonName;
    private String phone;
    private String email;
    private String address;
    private String cityCode;
    private String cityName;

    private List<String> cuisines;
    private boolean isVegetarian;

    private int priceRange;
    private int averageDeliveryTimeMinutes;
    private double minimumOrderAmount;

    private double averageRating;
    private int totalRatings;
    private int totalOrders;
    private double popularityScore;

    private double latitude;
    private double longitude;

    private List<OperatingHourResponse> operatingHours;
    private boolean openNow;

    private boolean isActive;
    private boolean isVerified;

    private List<String> images;

    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    // Getters and Setters

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    public String getContactPersonName() { return contactPersonName; }
    public void setContactPersonName(String contactPersonName) { this.contactPersonName = contactPersonName; }

    public String getPhone() { return phone; }
    public void setPhone(String phone) { this.phone = phone; }

    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }

    public String getAddress() { return address; }
    public void setAddress(String address) { this.address = address; }

    public String getCityCode() { return cityCode; }
    public void setCityCode(String cityCode) { this.cityCode = cityCode; }

    public String getCityName() { return cityName; }
    public void setCityName(String cityName) { this.cityName = cityName; }

    public List<String> getCuisines() { return cuisines; }
    public void setCuisines(List<String> cuisines) { this.cuisines = cuisines; }

    public boolean isVegetarian() { return isVegetarian; }
    public void setVegetarian(boolean vegetarian) { isVegetarian = vegetarian; }

    public int getPriceRange() { return priceRange; }
    public void setPriceRange(int priceRange) { this.priceRange = priceRange; }

    public int getAverageDeliveryTimeMinutes() { return averageDeliveryTimeMinutes; }
    public void setAverageDeliveryTimeMinutes(int averageDeliveryTimeMinutes) { this.averageDeliveryTimeMinutes = averageDeliveryTimeMinutes; }

    public double getMinimumOrderAmount() { return minimumOrderAmount; }
    public void setMinimumOrderAmount(double minimumOrderAmount) { this.minimumOrderAmount = minimumOrderAmount; }

    public double getAverageRating() { return averageRating; }
    public void setAverageRating(double averageRating) { this.averageRating = averageRating; }

    public int getTotalRatings() { return totalRatings; }
    public void setTotalRatings(int totalRatings) { this.totalRatings = totalRatings; }

    public int getTotalOrders() { return totalOrders; }
    public void setTotalOrders(int totalOrders) { this.totalOrders = totalOrders; }

    public double getPopularityScore() { return popularityScore; }
    public void setPopularityScore(double popularityScore) { this.popularityScore = popularityScore; }

    public double getLatitude() { return latitude; }
    public void setLatitude(double latitude) { this.latitude = latitude; }

    public double getLongitude() { return longitude; }
    public void setLongitude(double longitude) { this.longitude = longitude; }

    public List<OperatingHourResponse> getOperatingHours() { return operatingHours; }
    public void setOperatingHours(List<OperatingHourResponse> operatingHours) { this.operatingHours = operatingHours; }

    public boolean isOpenNow() { return openNow; }
    public void setOpenNow(boolean openNow) { this.openNow = openNow; }

    public boolean isActive() { return isActive; }
    public void setActive(boolean active) { isActive = active; }

    public boolean isVerified() { return isVerified; }
    public void setVerified(boolean verified) { isVerified = verified; }

    public List<String> getImages() { return images; }
    public void setImages(List<String> images) { this.images = images; }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }

    public LocalDateTime getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(LocalDateTime updatedAt) { this.updatedAt = updatedAt; }
}
