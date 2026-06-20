package com.fooddelivery.tracking.service;

import com.fooddelivery.tracking.dto.TimelineEventResponse;
import org.springframework.data.mongodb.core.geo.GeoJsonPoint;

import java.util.List;

public interface TrackingService {

    List<TimelineEventResponse> getVisibleTimeline(String orderId, String email);

    void logLocationHistory(String driverId, String orderId, GeoJsonPoint location);

    GeoJsonPoint getFilteredDriverLocation(String orderId, String email);
}
