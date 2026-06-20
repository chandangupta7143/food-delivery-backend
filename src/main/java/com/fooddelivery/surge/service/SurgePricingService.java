package com.fooddelivery.surge.service;

import com.fooddelivery.surge.dto.*;

public interface SurgePricingService {

    PricingQuoteResponse calculateQuote(PricingQuoteRequest request);

    AdminSurgeOverrideResponse createOverride(AdminSurgeOverrideRequest request, String adminEmail);

    void removeOverride(String overrideId);

    void setEmergencyDisable(boolean disable, String zoneName);

    double getActiveSurgeMultiplier(String h3Index);
}
