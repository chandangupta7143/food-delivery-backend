package com.fooddelivery.surge.service;

public interface SupplyAnalysisEngine {

    void registerDriverLocation(String driverId, double lat, double lng, String status);

    long getAvailableDriversCount(String h3Index);

    double calculateSupplyScore(String h3Index, long pendingOrdersCount);

    double calculateDriverPressureIndex(String h3Index, long pendingOrdersCount, long checkoutAttempts);
}
