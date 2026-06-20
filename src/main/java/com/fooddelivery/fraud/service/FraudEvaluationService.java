package com.fooddelivery.fraud.service;

import com.fooddelivery.orders.entity.Order;

public interface FraudEvaluationService {
    /**
     * Evaluates the risk score and fraud flags of an order synchronously.
     * Enforces SLA time bounds and fallback states.
     */
    void evaluateOrder(Order order);
}
