package com.nitrotech.api.application.order.controller;

import com.nitrotech.api.application.order.request.CreateOrderRequest;
import com.nitrotech.api.application.order.request.UpdateOrderStatusRequest;
import com.nitrotech.api.domain.order.dto.CreateOrderCommand;
import com.nitrotech.api.domain.order.dto.OrderData;
import com.nitrotech.api.domain.order.dto.OrderListQuery;
import com.nitrotech.api.domain.order.usecase.*;
import com.nitrotech.api.shared.response.ApiResult;
import com.nitrotech.api.shared.security.UserPrincipal;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/orders")
@Tag(name = "Orders", description = "Order placement and management APIs")
@RequiredArgsConstructor
public class OrderController {

    private final PlaceOrderUseCase placeOrderUseCase;
    private final GetOrdersUseCase getOrdersUseCase;
    private final GetOrderUseCase getOrderUseCase;
    private final CancelOrderUseCase cancelOrderUseCase;
    private final UpdateOrderStatusUseCase updateOrderStatusUseCase;

    @Operation(summary = "List orders", description = "Get paginated list of orders for the current user, optionally filtered by status.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Orders retrieved successfully"),
            @ApiResponse(responseCode = "401", description = "Not authenticated", content = @Content(mediaType = "application/json"))
    })
    @GetMapping
    public ResponseEntity<ApiResult<List<OrderData>>> list(
            @AuthenticationPrincipal UserPrincipal principal,
            @Parameter(description = "Filter by order status (e.g. pending, confirmed, shipped, delivered, cancelled)") @RequestParam(required = false) String status,
            @Parameter(description = "Page number (0-based)") @RequestParam(defaultValue = "0") int page,
            @Parameter(description = "Page size") @RequestParam(defaultValue = "10") int size
    ) {
        return ResponseEntity.ok(getOrdersUseCase.execute(
                new OrderListQuery(principal.id(), status, page, size)));
    }

    @Operation(summary = "Get order by ID", description = "Retrieve a single order. Users can only access their own orders.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Order found"),
            @ApiResponse(responseCode = "401", description = "Not authenticated", content = @Content(mediaType = "application/json")),
            @ApiResponse(responseCode = "404", description = "Order not found", content = @Content(mediaType = "application/json"))
    })
    @GetMapping("/{id}")
    public ResponseEntity<ApiResult<OrderData>> get(
            @AuthenticationPrincipal UserPrincipal principal,
            @Parameter(description = "Order ID") @PathVariable Long id
    ) {
        return ResponseEntity.ok(ApiResult.ok(getOrderUseCase.execute(id, principal.id())));
    }

    @Operation(summary = "Place order", description = "Create a new order from the current user's cart. Clears the cart on success.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "201", description = "Order placed successfully"),
            @ApiResponse(responseCode = "400", description = "Invalid input or cart is empty", content = @Content(mediaType = "application/json")),
            @ApiResponse(responseCode = "401", description = "Not authenticated", content = @Content(mediaType = "application/json")),
            @ApiResponse(responseCode = "404", description = "Address or promotion not found", content = @Content(mediaType = "application/json")),
            @ApiResponse(responseCode = "409", description = "Insufficient stock for one or more items", content = @Content(mediaType = "application/json"))
    })
    @PostMapping
    public ResponseEntity<ApiResult<OrderData>> place(
            @AuthenticationPrincipal UserPrincipal principal,
            @Valid @RequestBody CreateOrderRequest req
    ) {
        String paymentMethod = req.paymentMethod() != null ? req.paymentMethod() : "cod";
        OrderData order = placeOrderUseCase.execute(new CreateOrderCommand(
                principal.id(), req.addressId(), paymentMethod, req.promotionCode(), req.note()));
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResult.created(order));
    }

    @Operation(summary = "Cancel order", description = "Cancel an order. Only orders in pending or confirmed status can be cancelled.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Order cancelled successfully"),
            @ApiResponse(responseCode = "401", description = "Not authenticated", content = @Content(mediaType = "application/json")),
            @ApiResponse(responseCode = "404", description = "Order not found", content = @Content(mediaType = "application/json")),
            @ApiResponse(responseCode = "409", description = "Order cannot be cancelled in its current status", content = @Content(mediaType = "application/json"))
    })
    @PatchMapping("/{id}/cancel")
    public ResponseEntity<ApiResult<OrderData>> cancel(
            @AuthenticationPrincipal UserPrincipal principal,
            @Parameter(description = "Order ID") @PathVariable Long id
    ) {
        return ResponseEntity.ok(ApiResult.ok(cancelOrderUseCase.execute(id, principal.id())));
    }

    @Operation(summary = "Update order status (admin)", description = "Admin endpoint. Update the status of any order (e.g. confirmed → shipped → delivered).")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Order status updated"),
            @ApiResponse(responseCode = "400", description = "Invalid status transition", content = @Content(mediaType = "application/json")),
            @ApiResponse(responseCode = "401", description = "Not authenticated", content = @Content(mediaType = "application/json")),
            @ApiResponse(responseCode = "403", description = "Insufficient permissions", content = @Content(mediaType = "application/json")),
            @ApiResponse(responseCode = "404", description = "Order not found", content = @Content(mediaType = "application/json"))
    })
    @PatchMapping("/{id}/status")
    public ResponseEntity<ApiResult<OrderData>> updateStatus(
            @Parameter(description = "Order ID") @PathVariable Long id,
            @Valid @RequestBody UpdateOrderStatusRequest req
    ) {
        return ResponseEntity.ok(ApiResult.ok(updateOrderStatusUseCase.execute(id, req.status())));
    }
}
