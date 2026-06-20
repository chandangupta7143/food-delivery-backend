package com.fooddelivery.restaurants.dto;

public class OperatingHourResponse {

    private String day;
    private String openTime;
    private String closeTime;

    public String getDay() { return day; }
    public void setDay(String day) { this.day = day; }

    public String getOpenTime() { return openTime; }
    public void setOpenTime(String openTime) { this.openTime = openTime; }

    public String getCloseTime() { return closeTime; }
    public void setCloseTime(String closeTime) { this.closeTime = closeTime; }
}
