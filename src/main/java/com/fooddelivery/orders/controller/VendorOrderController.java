package com.fooddelivery.orders.controller;

import com.fooddelivery.common.response.ApiResponse;
import com.fooddelivery.orders.dto.OrderResponse;
import com.fooddelivery.orders.dto.OrderStatusUpdateRequest;
import com.fooddelivery.orders.service.OrderService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.security.Principal;
import java.util.List;

@RestController
@RequestMapping("/api/vendor/orders")
@Validated
public class VendorOrderController {

    private final OrderService orderService;

    public VendorOrderController(OrderService orderService) {
        this.orderService = orderService;
    }

    @GetMapping("/active")
    public ResponseEntity<ApiResponse<List<OrderResponse>>> getActiveOrders(
            @RequestParam String restaurantId) {
        List<OrderResponse> response = orderService.getActiveVendorOrders(restaurantId);
        return ResponseEntity.ok(ApiResponse.success("Active restaurant orders retrieved successfully", response));
    }

    @PatchMapping("/{id}/status")
    public ResponseEntity<ApiResponse<OrderResponse>> updateOrderStatus(
            @PathVariable String id,
            @Valid @RequestBody OrderStatusUpdateRequest request,
            Principal principal) {
        OrderResponse response = orderService.updateOrderStatus(id, request.getStatus(), principal.getName(), "RESTAURANT");
        return ResponseEntity.ok(ApiResponse.success("Order status updated successfully", response));
    }
}
