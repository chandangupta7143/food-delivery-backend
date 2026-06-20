package com.fooddelivery.fraud.service;

import com.fooddelivery.orders.dto.OrderResponse;
import com.fooddelivery.fraud.entity.DailyFraudMetrics;
import java.util.List;

public interface FraudAdminService {
    
    /**
     * Retrieves all orders currently in PENDING_REVIEW state.
     */
    List<OrderResponse> getFraudQueue();

    /**
     * Approves an order held in PENDING_REVIEW.
     * Captures payment, records audit log, and transitions status to CREATED.
     * adminOverrideReason is mandatory and must be >= 10 characters.
     */
    OrderResponse approveOrder(String orderId, String adminEmail, String reason);

    /**
     * Rejects an order held in PENDING_REVIEW.
     * Releases payment hold, records audit log, and transitions status to REJECTED.
     * adminOverrideReason is mandatory and must be >= 10 characters.
     */
    OrderResponse rejectOrder(String orderId, String adminEmail, String reason);

    /**
     * Manually restricts a user account (e.g. banning coupons, refunds, or ordering).
     */
    void restrictUser(String userId, String adminEmail, List<String> restrictionTypes, String reason, int durationDays);

    /**
     * Retrieves daily aggregated metrics for a specific date.
     */
    DailyFraudMetrics getDailyMetrics(String dateStr);
}
