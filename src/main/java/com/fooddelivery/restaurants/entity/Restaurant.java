package com.fooddelivery.restaurants.entity;

import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.mongodb.core.geo.GeoJsonPoint;
import org.springframework.data.mongodb.core.index.CompoundIndex;
import org.springframework.data.mongodb.core.index.CompoundIndexes;
import org.springframework.data.mongodb.core.index.GeoSpatialIndexType;
import org.springframework.data.mongodb.core.index.GeoSpatialIndexed;
import org.springframework.data.mongodb.core.index.TextIndexed;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * Restaurant domain entity stored in the 'restaurants' MongoDB collection.
 */
@Document(collection = "restaurants")
@CompoundIndexes({
        @CompoundIndex(name = "idx_public_base", def = "{'isActive': 1, 'isVerified': 1, 'isDeleted': 1}"),
        @CompoundIndex(name = "idx_public_cuisines", def = "{'isActive': 1, 'isVerified': 1, 'isDeleted': 1, 'cuisines': 1}"),
        @CompoundIndex(name = "idx_public_rating", def = "{'isActive': 1, 'isVerified': 1, 'isDeleted': 1, 'averageRating': -1}"),
        @CompoundIndex(name = "idx_public_city", def = "{'isActive': 1, 'isVerified': 1, 'isDeleted': 1, 'cityCode': 1}"),
        @CompoundIndex(name = "idx_public_popularity", def = "{'isActive': 1, 'isVerified': 1, 'isDeleted': 1, 'popularityScore': -1}"),
        @CompoundIndex(name = "idx_public_delivery", def = "{'isActive': 1, 'isVerified': 1, 'isDeleted': 1, 'averageDeliveryTimeMinutes': 1}"),
        @CompoundIndex(name = "idx_active_verified", def = "{'isActive': 1, 'isVerified': 1}")
})
public class Restaurant {

    @Id
    private String id;

    @TextIndexed
    private String name;

    private String description;
    private String contactPersonName;
    private String phone;
    private String email;
    private String address;
    private String cityCode;
    private String cityName;

    private List<String> cuisines = new ArrayList<>();
    private boolean isVegetarian;

    private int priceRange; // 1-4
    private int averageDeliveryTimeMinutes;
    private double minimumOrderAmount;

    private List<OperatingHour> operatingHours = new ArrayList<>();

    private double averageRating;
    private int totalRatings;
    private int totalOrders;
    private double popularityScore;

    @GeoSpatialIndexed(type = GeoSpatialIndexType.GEO_2DSPHERE)
    private GeoJsonPoint location;

    private boolean isActive;
    private boolean isVerified;
    private boolean isDeleted;

    private List<String> images = new ArrayList<>();

    @CreatedDate
    private LocalDateTime createdAt;

    @LastModifiedDate
    private LocalDateTime updatedAt;

    public Restaurant() {
        this.isActive = true;
        this.isVerified = false;
        this.isDeleted = false;
        this.averageRating = 0.0;
        this.totalRatings = 0;
        this.totalOrders = 0;
        this.popularityScore = 0.0;
    }

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

    public List<OperatingHour> getOperatingHours() { return operatingHours; }
    public void setOperatingHours(List<OperatingHour> operatingHours) { this.operatingHours = operatingHours; }

    public double getAverageRating() { return averageRating; }
    public void setAverageRating(double averageRating) { this.averageRating = averageRating; }

    public int getTotalRatings() { return totalRatings; }
    public void setTotalRatings(int totalRatings) { this.totalRatings = totalRatings; }

    public int getTotalOrders() { return totalOrders; }
    public void setTotalOrders(int totalOrders) { this.totalOrders = totalOrders; }

    public double getPopularityScore() { return popularityScore; }
    public void setPopularityScore(double popularityScore) { this.popularityScore = popularityScore; }

    public GeoJsonPoint getLocation() { return location; }
    public void setLocation(GeoJsonPoint location) { this.location = location; }

    public boolean getIsActive() { return isActive; }
    public void setIsActive(boolean isActive) { this.isActive = isActive; }

    public boolean getIsVerified() { return isVerified; }
    public void setIsVerified(boolean isVerified) { this.isVerified = isVerified; }

    public boolean getIsDeleted() { return isDeleted; }
    public void setIsDeleted(boolean isDeleted) { this.isDeleted = isDeleted; }

    public List<String> getImages() { return images; }
    public void setImages(List<String> images) { this.images = images; }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }

    public LocalDateTime getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(LocalDateTime updatedAt) { this.updatedAt = updatedAt; }
}
