package com.fooddelivery.restaurants.controller;

import com.fooddelivery.common.response.ApiResponse;
import com.fooddelivery.common.response.PaginatedResponse;
import com.fooddelivery.restaurants.dto.NearbySearchRequest;
import com.fooddelivery.restaurants.dto.RestaurantResponse;
import com.fooddelivery.restaurants.dto.RestaurantSearchRequest;
import com.fooddelivery.restaurants.service.RestaurantService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;

@RestController
@RequestMapping("/api/restaurants")
@Validated
public class RestaurantController {

    private final RestaurantService restaurantService;

    public RestaurantController(RestaurantService restaurantService) {
        this.restaurantService = restaurantService;
    }

    @GetMapping
    public ResponseEntity<ApiResponse<PaginatedResponse<RestaurantResponse>>> searchRestaurants(
            @Valid @ModelAttribute RestaurantSearchRequest request) {
        PaginatedResponse<RestaurantResponse> response = restaurantService.searchRestaurants(request);
        return ResponseEntity.ok(ApiResponse.success("Restaurants retrieved successfully", response));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<RestaurantResponse>> getRestaurant(@PathVariable String id) {
        RestaurantResponse response = restaurantService.getRestaurantPublic(id);
        return ResponseEntity.ok(ApiResponse.success("Restaurant retrieved successfully", response));
    }

    @GetMapping("/search")
    public ResponseEntity<ApiResponse<PaginatedResponse<RestaurantResponse>>> searchByName(
            @RequestParam String name,
            @RequestParam(defaultValue = "0") @Min(value = 0, message = "Page must be at least 0") int page,
            @RequestParam(defaultValue = "20") @Min(value = 1, message = "Size must be at least 1") @Max(value = 50, message = "Size must be at most 50") int size) {
        PaginatedResponse<RestaurantResponse> response = restaurantService.searchByName(name, page, size);
        return ResponseEntity.ok(ApiResponse.success("Restaurants retrieved successfully", response));
    }

    @GetMapping("/nearby")
    public ResponseEntity<ApiResponse<PaginatedResponse<RestaurantResponse>>> searchNearby(
            @Valid @ModelAttribute NearbySearchRequest request) {
        PaginatedResponse<RestaurantResponse> response = restaurantService.searchNearby(request);
        return ResponseEntity.ok(ApiResponse.success("Nearby restaurants retrieved successfully", response));
    }
}
