package com.fooddelivery.restaurants.dto;

public class RestaurantAdminResponse extends RestaurantResponse {

    private boolean isDeleted;

    public boolean getIsDeleted() { return isDeleted; }
    public void setIsDeleted(boolean isDeleted) { this.isDeleted = isDeleted; }
}
