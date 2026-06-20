package com.fooddelivery.surge.service;

public interface DemandAnalysisEngine {

    void registerCheckoutAttempt(String h3Index);

    void registerCartAddition(String h3Index);

    void registerOrderPlaced(String h3Index);

    void registerSearchQuery(String h3Index);

    double calculateDemandScore(String h3Index);
}
