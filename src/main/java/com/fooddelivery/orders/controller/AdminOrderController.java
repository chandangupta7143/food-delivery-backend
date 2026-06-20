package com.fooddelivery.orders.controller;

import com.fooddelivery.common.response.ApiResponse;
import com.fooddelivery.orders.dto.FraudReviewRequest;
import com.fooddelivery.orders.dto.OrderResponse;
import com.fooddelivery.orders.service.OrderService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.security.Principal;

@RestController
@RequestMapping("/api/admin/orders")
@Validated
public class AdminOrderController {

    private final OrderService orderService;

    public AdminOrderController(OrderService orderService) {
        this.orderService = orderService;
    }

    @PatchMapping("/{id}/fraud-review")
    public ResponseEntity<ApiResponse<OrderResponse>> reviewFraud(
            @PathVariable String id,
            @Valid @RequestBody FraudReviewRequest request,
            Principal principal) {
        OrderResponse response = orderService.reviewFraud(id, request, principal.getName());
        return ResponseEntity.ok(ApiResponse.success("Order fraud review completed successfully", response));
    }

    @PatchMapping("/{id}/force-cancel")
    public ResponseEntity<ApiResponse<OrderResponse>> forceCancel(
            @PathVariable String id,
            Principal principal) {
        OrderResponse response = orderService.forceCancelOrder(id, principal.getName());
        return ResponseEntity.ok(ApiResponse.success("Order force-cancelled successfully", response));
    }
}
