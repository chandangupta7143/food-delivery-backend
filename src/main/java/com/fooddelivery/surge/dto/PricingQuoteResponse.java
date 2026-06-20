package com.fooddelivery.surge.dto;

public class PricingQuoteResponse {

    private double deliveryFee;
    private double baseFee;
    private double surgeMultiplier;
    private String quoteToken;
    private long expiresAt; // Epoch seconds timestamp

    public PricingQuoteResponse() {
    }

    public PricingQuoteResponse(double deliveryFee, double baseFee, double surgeMultiplier, String quoteToken, long expiresAt) {
        this.deliveryFee = deliveryFee;
        this.baseFee = baseFee;
        this.surgeMultiplier = surgeMultiplier;
        this.quoteToken = quoteToken;
        this.expiresAt = expiresAt;
    }

    // Getters and Setters

    public double getDeliveryFee() {
        return deliveryFee;
    }

    public void setDeliveryFee(double deliveryFee) {
        this.deliveryFee = deliveryFee;
    }

    public double getBaseFee() {
        return baseFee;
    }

    public void setBaseFee(double baseFee) {
        this.baseFee = baseFee;
    }

    public double getSurgeMultiplier() {
        return surgeMultiplier;
    }

    public void setSurgeMultiplier(double surgeMultiplier) {
        this.surgeMultiplier = surgeMultiplier;
    }

    public String getQuoteToken() {
        return quoteToken;
    }

    public void setQuoteToken(String quoteToken) {
        this.quoteToken = quoteToken;
    }

    public long getExpiresAt() {
        return expiresAt;
    }

    public void setExpiresAt(long expiresAt) {
        this.expiresAt = expiresAt;
    }
}
