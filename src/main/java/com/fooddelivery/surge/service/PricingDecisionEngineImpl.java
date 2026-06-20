package com.fooddelivery.surge.service;

import org.springframework.stereotype.Service;

@Service
public class PricingDecisionEngineImpl implements PricingDecisionEngine {

    private static final double ALPHA = 0.3; // Damping smoothing factor

    @Override
    public double getSmoothedMultiplier(String h3Index, double calculatedMultiplier, double previousMultiplier) {
        if (previousMultiplier <= 0.0) {
            return calculatedMultiplier; // first calculation, no smoothing
        }
        return applyDampingFilter(calculatedMultiplier, previousMultiplier);
    }

    @Override
    public double applyDampingFilter(double current, double previous) {
        // EMA: alpha * current + (1 - alpha) * previous
        double smoothed = (ALPHA * current) + ((1.0 - ALPHA) * previous);
        // Round to 2 decimal places for neat pricing multipliers
        return Math.round(smoothed * 100.0) / 100.0;
    }
}
