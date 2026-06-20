package com.fooddelivery.search.mapper;

import com.fooddelivery.restaurants.dto.OperatingHourResponse;
import com.fooddelivery.restaurants.entity.OperatingHour;
import com.fooddelivery.restaurants.entity.Restaurant;
import com.fooddelivery.search.dto.AdvancedSearchResponse;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

@Component
public class SearchMapper {

    private static final DateTimeFormatter TIME_FORMATTER = DateTimeFormatter.ofPattern("HH:mm");

    public AdvancedSearchResponse toResponse(Restaurant restaurant) {
        AdvancedSearchResponse response = new AdvancedSearchResponse();
        
        response.setId(restaurant.getId());
        response.setName(restaurant.getName());
        
        // Only map the first image for thumbnails to save bandwidth
        if (restaurant.getImages() != null && !restaurant.getImages().isEmpty()) {
            response.setImages(Collections.singletonList(restaurant.getImages().get(0)));
        }
        
        response.setCuisines(restaurant.getCuisines());
        response.setPriceRange(restaurant.getPriceRange());
        response.setVegetarian(restaurant.isVegetarian());
        
        // Recommendation compatibility fields
        response.setAverageRating(restaurant.getAverageRating());
        response.setTotalRatings(restaurant.getTotalRatings());
        response.setPopularityScore(restaurant.getPopularityScore());
        
        response.setAverageDeliveryTimeMinutes(restaurant.getAverageDeliveryTimeMinutes());
        
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
        
        return response;
    }

    private boolean calculateOpenNow(List<OperatingHour> operatingHours) {
        if (operatingHours == null || operatingHours.isEmpty()) {
            return false;
        }
        
        LocalDateTime now = LocalDateTime.now(ZoneId.of("UTC")); // Assuming UTC, can be made configurable
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
