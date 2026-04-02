package com.nitrotech.api.application.order.controller;

import com.nitrotech.api.application.order.request.CreateOrderRequest;
import com.nitrotech.api.application.order.request.UpdateOrderStatusRequest;
import com.nitrotech.api.domain.auth.usecase.GetProfileUseCase;
import com.nitrotech.api.domain.order.dto.CreateOrderCommand;
import com.nitrotech.api.domain.order.dto.OrderData;
import com.nitrotech.api.domain.order.dto.OrderListQuery;
import com.nitrotech.api.domain.order.usecase.*;
import com.nitrotech.api.shared.response.ApiResponse;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/orders")
public class OrderController {

    private final PlaceOrderUseCase placeOrderUseCase;
    private final GetOrdersUseCase getOrdersUseCase;
    private final GetOrderUseCase getOrderUseCase;
    private final CancelOrderUseCase cancelOrderUseCase;
    private final UpdateOrderStatusUseCase updateOrderStatusUseCase;
    private final GetProfileUseCase getProfileUseCase;

    public OrderController(PlaceOrderUseCase placeOrderUseCase, GetOrdersUseCase getOrdersUseCase,
                            GetOrderUseCase getOrderUseCase, CancelOrderUseCase cancelOrderUseCase,
                            UpdateOrderStatusUseCase updateOrderStatusUseCase,
                            GetProfileUseCase getProfileUseCase) {
        this.placeOrderUseCase = placeOrderUseCase;
        this.getOrdersUseCase = getOrdersUseCase;
        this.getOrderUseCase = getOrderUseCase;
        this.cancelOrderUseCase = cancelOrderUseCase;
        this.updateOrderStatusUseCase = updateOrderStatusUseCase;
        this.getProfileUseCase = getProfileUseCase;
    }

    @GetMapping
    public ResponseEntity<ApiResponse<List<OrderData>>> list(
            @AuthenticationPrincipal String email,
            @RequestParam(required = false) String status,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size
    ) {
        return ResponseEntity.ok(getOrdersUseCase.execute(
                new OrderListQuery(userId(email), status, page, size)));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<OrderData>> get(
            @AuthenticationPrincipal String email,
            @PathVariable Long id
    ) {
        return ResponseEntity.ok(ApiResponse.ok(getOrderUseCase.execute(id, userId(email))));
    }

    @PostMapping
    public ResponseEntity<ApiResponse<OrderData>> place(
            @AuthenticationPrincipal String email,
            @Valid @RequestBody CreateOrderRequest req
    ) {
        String paymentMethod = req.paymentMethod() != null ? req.paymentMethod() : "cod";
        OrderData order = placeOrderUseCase.execute(new CreateOrderCommand(
                userId(email), req.addressId(), paymentMethod, req.promotionCode(), req.note()));
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.created(order));
    }

    @PatchMapping("/{id}/cancel")
    public ResponseEntity<ApiResponse<OrderData>> cancel(
            @AuthenticationPrincipal String email,
            @PathVariable Long id
    ) {
        return ResponseEntity.ok(ApiResponse.ok(cancelOrderUseCase.execute(id, userId(email))));
    }

    // Admin only
    @PatchMapping("/{id}/status")
    public ResponseEntity<ApiResponse<OrderData>> updateStatus(
            @PathVariable Long id,
            @Valid @RequestBody UpdateOrderStatusRequest req
    ) {
        return ResponseEntity.ok(ApiResponse.ok(updateOrderStatusUseCase.execute(id, req.status())));
    }

    private Long userId(String email) {
        return getProfileUseCase.executeByEmail(email).id();
    }
}
