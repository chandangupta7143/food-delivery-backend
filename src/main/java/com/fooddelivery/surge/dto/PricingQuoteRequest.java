package com.fooddelivery.surge.dto;

import jakarta.validation.constraints.NotNull;

public class PricingQuoteRequest {

    @NotNull(message = "cartId is mandatory")
    private String cartId;

    @NotNull(message = "restaurantId is mandatory")
    private String restaurantId;

    @NotNull(message = "deliveryLatitude is mandatory")
    private Double deliveryLatitude;

    @NotNull(message = "deliveryLongitude is mandatory")
    private Double deliveryLongitude;

    // Getters and Setters

    public String getCartId() {
        return cartId;
    }

    public void setCartId(String cartId) {
        this.cartId = cartId;
    }

    public String getRestaurantId() {
        return restaurantId;
    }

    public void setRestaurantId(String restaurantId) {
        this.restaurantId = restaurantId;
    }

    public Double getDeliveryLatitude() {
        return deliveryLatitude;
    }

    public void setDeliveryLatitude(Double deliveryLatitude) {
        this.deliveryLatitude = deliveryLatitude;
    }

    public Double getDeliveryLongitude() {
        return deliveryLongitude;
    }

    public void setDeliveryLongitude(Double deliveryLongitude) {
        this.deliveryLongitude = deliveryLongitude;
    }
}
