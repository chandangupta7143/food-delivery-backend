package com.fooddelivery.surge.service;

public interface SurgeCalculationEngine {

    double calculateSurgeMultiplier(String h3Index, double demandScore, double supplyScore, double dpi,
                                    double threshold, double scaleFactor, double maxCap, double baseMultiplier);
}
