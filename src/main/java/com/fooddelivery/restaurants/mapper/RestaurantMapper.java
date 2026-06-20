package com.fooddelivery.restaurants.mapper;

import com.fooddelivery.restaurants.dto.*;
import com.fooddelivery.restaurants.entity.OperatingHour;
import com.fooddelivery.restaurants.entity.Restaurant;
import org.springframework.data.mongodb.core.geo.GeoJsonPoint;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.stream.Collectors;

@Component
public class RestaurantMapper {

    private static final DateTimeFormatter TIME_FORMATTER = DateTimeFormatter.ofPattern("HH:mm");

    public Restaurant toEntity(CreateRestaurantRequest request) {
        Restaurant restaurant = new Restaurant();
        restaurant.setName(request.getName());
        restaurant.setDescription(request.getDescription());
        restaurant.setContactPersonName(request.getContactPersonName());
        restaurant.setPhone(request.getPhone());
        restaurant.setEmail(request.getEmail());
        restaurant.setAddress(request.getAddress());
        restaurant.setCityCode(request.getCityCode().toUpperCase());
        restaurant.setCityName(request.getCityName());
        
        restaurant.setCuisines(request.getCuisines().stream().map(String::toLowerCase).collect(Collectors.toList()));
        restaurant.setVegetarian(request.getIsVegetarian());
        restaurant.setPriceRange(request.getPriceRange());
        restaurant.setAverageDeliveryTimeMinutes(request.getAverageDeliveryTimeMinutes());
        restaurant.setMinimumOrderAmount(request.getMinimumOrderAmount());
        
        if (request.getOperatingHours() != null) {
            restaurant.setOperatingHours(request.getOperatingHours().stream()
                    .map(h -> new OperatingHour(h.getDay().toUpperCase(), h.getOpenTime(), h.getCloseTime()))
                    .collect(Collectors.toList()));
        }
        
        if (request.getLongitude() != null && request.getLatitude() != null) {
            restaurant.setLocation(new GeoJsonPoint(request.getLongitude(), request.getLatitude()));
        }
        
        if (request.getImages() != null) {
            restaurant.setImages(request.getImages());
        }
        
        return restaurant;
    }

    public RestaurantResponse toResponse(Restaurant restaurant) {
        RestaurantResponse response = new RestaurantResponse();
        populateResponseFields(restaurant, response);
        return response;
    }

    public RestaurantAdminResponse toAdminResponse(Restaurant restaurant) {
        RestaurantAdminResponse response = new RestaurantAdminResponse();
        populateResponseFields(restaurant, response);
        response.setIsDeleted(restaurant.getIsDeleted());
        return response;
    }

    private void populateResponseFields(Restaurant restaurant, RestaurantResponse response) {
        response.setId(restaurant.getId());
        response.setName(restaurant.getName());
        response.setDescription(restaurant.getDescription());
        response.setContactPersonName(restaurant.getContactPersonName());
        response.setPhone(restaurant.getPhone());
        response.setEmail(restaurant.getEmail());
        response.setAddress(restaurant.getAddress());
        response.setCityCode(restaurant.getCityCode());
        response.setCityName(restaurant.getCityName());
        response.setCuisines(restaurant.getCuisines());
        response.setVegetarian(restaurant.isVegetarian());
        response.setPriceRange(restaurant.getPriceRange());
        response.setAverageDeliveryTimeMinutes(restaurant.getAverageDeliveryTimeMinutes());
        response.setMinimumOrderAmount(restaurant.getMinimumOrderAmount());
        response.setAverageRating(restaurant.getAverageRating());
        response.setTotalRatings(restaurant.getTotalRatings());
        response.setTotalOrders(restaurant.getTotalOrders());
        response.setPopularityScore(restaurant.getPopularityScore());
        
        if (restaurant.getLocation() != null) {
            response.setLongitude(restaurant.getLocation().getX());
            response.setLatitude(restaurant.getLocation().getY());
        }
        
        if (restaurant.getOperatingHours() != null) {
            List<OperatingHourResponse> hours = restaurant.getOperatingHours().stream().map(h -> {
                OperatingHourResponse r = new OperatingHourResponse();
                r.setDay(h.getDay());
                r.setOpenTime(h.getOpenTime());
                r.setCloseTime(h.getCloseTime());
                return r;
            }).collect(Collectors.toList());
            response.setOperatingHours(hours);
        }
        
        response.setOpenNow(calculateOpenNow(restaurant.getOperatingHours()));
        response.setActive(restaurant.getIsActive());
        response.setVerified(restaurant.getIsVerified());
        response.setImages(restaurant.getImages());
        response.setCreatedAt(restaurant.getCreatedAt());
        response.setUpdatedAt(restaurant.getUpdatedAt());
    }

    private boolean calculateOpenNow(List<OperatingHour> operatingHours) {
        if (operatingHours == null || operatingHours.isEmpty()) {
            return false;
        }
        
        LocalDateTime now = LocalDateTime.now(ZoneId.of("UTC")); // Assuming UTC for system, can be configured
        String currentDay = now.getDayOfWeek().name();
        LocalTime currentTime = now.toLocalTime();
        
        for (OperatingHour hour : operatingHours) {
            if (hour.getDay().equalsIgnoreCase(currentDay)) {
                LocalTime openTime = LocalTime.parse(hour.getOpenTime(), TIME_FORMATTER);
                LocalTime closeTime = LocalTime.parse(hour.getCloseTime(), TIME_FORMATTER);
                
                if (!currentTime.isBefore(openTime) && currentTime.isBefore(closeTime)) {
                    return true;
                }
            }
        }
        return false;
    }
}
