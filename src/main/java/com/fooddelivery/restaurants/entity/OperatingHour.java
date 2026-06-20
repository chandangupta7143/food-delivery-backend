package com.fooddelivery.restaurants.entity;

/**
 * Embedded entity representing operating hours for a specific day.
 */
public class OperatingHour {

    private String day; // MONDAY, TUESDAY, etc.
    private String openTime; // HH:mm format (e.g., "09:00")
    private String closeTime; // HH:mm format (e.g., "23:00")

    public OperatingHour() {
    }

    public OperatingHour(String day, String openTime, String closeTime) {
        this.day = day;
        this.openTime = openTime;
        this.closeTime = closeTime;
    }

    public String getDay() {
        return day;
    }

    public void setDay(String day) {
        this.day = day;
    }

    public String getOpenTime() {
        return openTime;
    }

    public void setOpenTime(String openTime) {
        this.openTime = openTime;
    }

    public String getCloseTime() {
        return closeTime;
    }

    public void setCloseTime(String closeTime) {
        this.closeTime = closeTime;
    }
}
