package com.fooddelivery.orders.entity;

/**
 * Embedded document representing a rule snapshot contributing to the final risk score.
 */
public class TriggeredRule {

    private String ruleName;
    private double contribution;

    public TriggeredRule() {
    }

    public TriggeredRule(String ruleName, double contribution) {
        this.ruleName = ruleName;
        this.contribution = contribution;
    }

    public String getRuleName() {
        return ruleName;
    }

    public void setRuleName(String ruleName) {
        this.ruleName = ruleName;
    }

    public double getContribution() {
        return contribution;
    }

    public void setContribution(double contribution) {
        this.contribution = contribution;
    }
}
