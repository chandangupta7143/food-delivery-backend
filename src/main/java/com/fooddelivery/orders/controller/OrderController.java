package com.fooddelivery.orders.controller;

import com.fooddelivery.common.response.ApiResponse;
import com.fooddelivery.common.response.PaginatedResponse;
import com.fooddelivery.orders.dto.CreateOrderRequest;
import com.fooddelivery.orders.dto.OrderResponse;
import com.fooddelivery.orders.service.OrderService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.security.Principal;

@RestController
@RequestMapping("/api/orders")
@Validated
public class OrderController {

    private final OrderService orderService;

    public OrderController(OrderService orderService) {
        this.orderService = orderService;
    }

    @PostMapping
    public ResponseEntity<ApiResponse<OrderResponse>> createOrder(
            @Valid @RequestBody CreateOrderRequest request,
            @RequestHeader(value = "Idempotency-Key", required = false) String idempotencyKey,
            Principal principal) {
        OrderResponse response = orderService.createOrder(request, principal.getName(), idempotencyKey);
        return ResponseEntity.ok(ApiResponse.success("Order created successfully", response));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<OrderResponse>> getOrderById(
            @PathVariable String id,
            Principal principal) {
        OrderResponse response = orderService.getOrderById(id, principal.getName());
        return ResponseEntity.ok(ApiResponse.success("Order details retrieved successfully", response));
    }

    @GetMapping
    public ResponseEntity<ApiResponse<PaginatedResponse<OrderResponse>>> getMyOrders(
            @RequestParam(defaultValue = "0") @Min(value = 0, message = "Page must be at least 0") int page,
            @RequestParam(defaultValue = "10") @Min(value = 1, message = "Size must be at least 1") @Max(value = 50, message = "Size must be at most 50") int size,
            Principal principal) {
        PaginatedResponse<OrderResponse> response = orderService.getMyOrders(principal.getName(), page, size);
        return ResponseEntity.ok(ApiResponse.success("Order history retrieved successfully", response));
    }

    @PatchMapping("/{id}/cancel")
    public ResponseEntity<ApiResponse<OrderResponse>> cancelOrder(
            @PathVariable String id,
            Principal principal) {
        OrderResponse response = orderService.cancelOrder(id, principal.getName());
        return ResponseEntity.ok(ApiResponse.success("Order cancelled successfully", response));
    }
}
