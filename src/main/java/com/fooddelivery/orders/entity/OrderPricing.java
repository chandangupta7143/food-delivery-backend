package com.fooddelivery.orders.entity;

/**
 * Embedded document representing the permanent, immutable pricing breakdown of an order.
 */
public class OrderPricing {

    private double itemTotal;
    private double discount;
    private double subtotal;
    private double taxes;
    private double platformFee;
    private double deliveryFee;
    private double surgeFee;
    private double finalPayable;

    public OrderPricing() {
    }

    public OrderPricing(double itemTotal, double discount, double subtotal, double taxes,
                        double platformFee, double deliveryFee, double surgeFee, double finalPayable) {
        this.itemTotal = itemTotal;
        this.discount = discount;
        this.subtotal = subtotal;
        this.taxes = taxes;
        this.platformFee = platformFee;
        this.deliveryFee = deliveryFee;
        this.surgeFee = surgeFee;
        this.finalPayable = finalPayable;
    }

    // Getters and Setters

    public double getItemTotal() {
        return itemTotal;
    }

    public void setItemTotal(double itemTotal) {
        this.itemTotal = itemTotal;
    }

    public double getDiscount() {
        return discount;
    }

    public void setDiscount(double discount) {
        this.discount = discount;
    }

    public double getSubtotal() {
        return subtotal;
    }

    public void setSubtotal(double subtotal) {
        this.subtotal = subtotal;
    }

    public double getTaxes() {
        return taxes;
    }

    public void setTaxes(double taxes) {
        this.taxes = taxes;
    }

    public double getPlatformFee() {
        return platformFee;
    }

    public void setPlatformFee(double platformFee) {
        this.platformFee = platformFee;
    }

    public double getDeliveryFee() {
        return deliveryFee;
    }

    public void setDeliveryFee(double deliveryFee) {
        this.deliveryFee = deliveryFee;
    }

    public double getSurgeFee() {
        return surgeFee;
    }

    public void setSurgeFee(double surgeFee) {
        this.surgeFee = surgeFee;
    }

    public double getFinalPayable() {
        return finalPayable;
    }

    public void setFinalPayable(double finalPayable) {
        this.finalPayable = finalPayable;
    }
}
