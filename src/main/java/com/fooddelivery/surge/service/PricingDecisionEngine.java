package com.fooddelivery.surge.service;

public interface PricingDecisionEngine {

    double getSmoothedMultiplier(String h3Index, double calculatedMultiplier, double previousMultiplier);

    double applyDampingFilter(double current, double previous);
}
