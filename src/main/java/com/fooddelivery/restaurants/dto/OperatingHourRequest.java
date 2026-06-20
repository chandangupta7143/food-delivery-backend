package com.fooddelivery.restaurants.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

import com.fooddelivery.restaurants.validator.OperatingHourValidator;

@OperatingHourValidator
public class OperatingHourRequest {

    @NotBlank(message = "Day is required")
    @Pattern(regexp = "^(MONDAY|TUESDAY|WEDNESDAY|THURSDAY|FRIDAY|SATURDAY|SUNDAY)$", message = "Invalid day of week")
    private String day;

    @NotBlank(message = "Open time is required")
    @Pattern(regexp = "^([01]\\d|2[0-3]):([0-5]\\d)$", message = "Invalid time format. Expected HH:mm")
    private String openTime;

    @NotBlank(message = "Close time is required")
    @Pattern(regexp = "^([01]\\d|2[0-3]):([0-5]\\d)$", message = "Invalid time format. Expected HH:mm")
    private String closeTime;

    public String getDay() { return day; }
    public void setDay(String day) { this.day = day; }

    public String getOpenTime() { return openTime; }
    public void setOpenTime(String openTime) { this.openTime = openTime; }

    public String getCloseTime() { return closeTime; }
    public void setCloseTime(String closeTime) { this.closeTime = closeTime; }
}
