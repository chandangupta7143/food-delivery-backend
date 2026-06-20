package com.fooddelivery.restaurants.controller;

import com.fooddelivery.common.response.ApiResponse;
import com.fooddelivery.common.response.PaginatedResponse;
import com.fooddelivery.restaurants.dto.CreateRestaurantRequest;
import com.fooddelivery.restaurants.dto.RestaurantAdminResponse;
import com.fooddelivery.restaurants.dto.UpdateRestaurantRequest;
import com.fooddelivery.restaurants.service.RestaurantService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;

@RestController
@RequestMapping("/api/admin/restaurants")
@Validated
public class AdminRestaurantController {

    private final RestaurantService restaurantService;

    public AdminRestaurantController(RestaurantService restaurantService) {
        this.restaurantService = restaurantService;
    }

    @PostMapping
    public ResponseEntity<ApiResponse<RestaurantAdminResponse>> createRestaurant(@Valid @RequestBody CreateRestaurantRequest request) {
        RestaurantAdminResponse response = restaurantService.createRestaurant(request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success("Restaurant created successfully", response));
    }

    @PutMapping("/{id}")
    public ResponseEntity<ApiResponse<RestaurantAdminResponse>> updateRestaurant(
            @PathVariable String id, 
            @Valid @RequestBody UpdateRestaurantRequest request) {
        RestaurantAdminResponse response = restaurantService.updateRestaurant(id, request);
        return ResponseEntity.ok(ApiResponse.success("Restaurant updated successfully", response));
    }

    @PatchMapping("/{id}/deactivate")
    public ResponseEntity<ApiResponse<Void>> deactivateRestaurant(@PathVariable String id) {
        restaurantService.deactivateRestaurant(id);
        return ResponseEntity.ok(ApiResponse.success("Restaurant deactivated successfully"));
    }

    @PatchMapping("/{id}/activate")
    public ResponseEntity<ApiResponse<Void>> activateRestaurant(@PathVariable String id) {
        restaurantService.activateRestaurant(id);
        return ResponseEntity.ok(ApiResponse.success("Restaurant activated successfully"));
    }

    @PatchMapping("/{id}/verify")
    public ResponseEntity<ApiResponse<Void>> verifyRestaurant(@PathVariable String id) {
        restaurantService.verifyRestaurant(id);
        return ResponseEntity.ok(ApiResponse.success("Restaurant verified successfully"));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<ApiResponse<Void>> deleteRestaurant(@PathVariable String id) {
        restaurantService.deleteRestaurant(id);
        return ResponseEntity.ok(ApiResponse.success("Restaurant deleted successfully"));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<RestaurantAdminResponse>> getRestaurant(@PathVariable String id) {
        RestaurantAdminResponse response = restaurantService.getRestaurantForAdmin(id);
        return ResponseEntity.ok(ApiResponse.success("Restaurant retrieved successfully", response));
    }

    @GetMapping
    public ResponseEntity<ApiResponse<PaginatedResponse<RestaurantAdminResponse>>> listRestaurants(
            @RequestParam(defaultValue = "0") @Min(value = 0, message = "Page must be at least 0") int page,
            @RequestParam(defaultValue = "20") @Min(value = 1, message = "Size must be at least 1") @Max(value = 50, message = "Size must be at most 50") int size) {
        PaginatedResponse<RestaurantAdminResponse> response = restaurantService.listRestaurantsForAdmin(page, size);
        return ResponseEntity.ok(ApiResponse.success("Restaurants retrieved successfully", response));
    }
}
