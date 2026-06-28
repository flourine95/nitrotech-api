package com.nitrotech.api.application.order.controller;

import com.nitrotech.api.application.order.request.CreateOrderRequest;
import com.nitrotech.api.application.order.request.OrderListRequest;
import com.nitrotech.api.application.order.request.UpdateOrderStatusRequest;
import com.nitrotech.api.domain.order.dto.CreateOrderCommand;
import com.nitrotech.api.domain.order.dto.OrderData;
import com.nitrotech.api.domain.order.dto.OrderListItemData;
import com.nitrotech.api.domain.order.dto.ShippingAddressSnapshot;
import com.nitrotech.api.domain.order.usecase.*;
import com.nitrotech.api.domain.shipping.dto.OrderShipmentData;
import com.nitrotech.api.domain.shipping.usecase.GetOrderShipmentUseCase;
import com.nitrotech.api.shared.response.ApiResult;
import com.nitrotech.api.shared.security.UserPrincipal;
import com.nitrotech.api.shared.validation.ValidSortFields;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/orders")
@RequiredArgsConstructor
@Validated
public class OrderController {

    private final PlaceOrderUseCase placeOrderUseCase;
    private final GetOrdersUseCase getOrdersUseCase;
    private final GetOrderUseCase getOrderUseCase;
    private final CancelOrderUseCase cancelOrderUseCase;
    private final UpdateOrderStatusUseCase updateOrderStatusUseCase;
    private final GetOrderShipmentUseCase getOrderShipmentUseCase;

    @GetMapping
    @PreAuthorize("hasAuthority('ORDER_READ_OWN')")
    public ResponseEntity<ApiResult<List<OrderListItemData>>> list(
            @AuthenticationPrincipal UserPrincipal principal,
            @Valid @ModelAttribute OrderListRequest filter,
            @PageableDefault(size = 10, sort = "createdAt", direction = Sort.Direction.DESC)
            @ValidSortFields({"id", "status", "paymentMethod", "finalAmount", "createdAt", "updatedAt"})
            Pageable pageable
    ) {
        return ResponseEntity.ok(ApiResult.paged(getOrdersUseCase.execute(
                filter.toCustomerFilter(principal.id()), pageable)));
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAuthority('ORDER_READ_OWN')")
    public ResponseEntity<ApiResult<OrderData>> get(
            @AuthenticationPrincipal UserPrincipal principal,
            @PathVariable Long id
    ) {
        return ResponseEntity.ok(ApiResult.ok(getOrderUseCase.execute(id, principal.id())));
    }

    @GetMapping("/{id}/shipment")
    @PreAuthorize("hasAuthority('ORDER_READ_OWN')")
    public ResponseEntity<ApiResult<OrderShipmentData>> getShipment(
            @AuthenticationPrincipal UserPrincipal principal,
            @PathVariable Long id
    ) {
        getOrderUseCase.execute(id, principal.id());
        return ResponseEntity.ok(ApiResult.ok(getOrderShipmentUseCase.execute(id)));
    }

    @PostMapping
    public ResponseEntity<ApiResult<OrderData>> place(
            @AuthenticationPrincipal UserPrincipal principal,
            @Valid @RequestBody CreateOrderRequest req
    ) {
        String paymentMethod = req.paymentMethod() != null ? req.paymentMethod() : "cod";
        ShippingAddressSnapshot shippingAddress = req.shippingAddress() == null ? null : new ShippingAddressSnapshot(
                req.shippingAddress().name(),
                req.shippingAddress().phone(),
                req.shippingAddress().city(),
                req.shippingAddress().cityCode() != null ? req.shippingAddress().cityCode() : "",
                req.shippingAddress().district(),
                req.shippingAddress().districtCode() != null ? req.shippingAddress().districtCode() : "",
                req.shippingAddress().ward(),
                req.shippingAddress().wardCode() != null ? req.shippingAddress().wardCode() : "",
                req.shippingAddress().address()
        );
        OrderData order = placeOrderUseCase.execute(new CreateOrderCommand(
                principal.id(), req.addressId(), shippingAddress, paymentMethod, req.promotionCode(), req.note()));
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResult.created(order));
    }

    @PatchMapping("/{id}/cancel")
    @PreAuthorize("hasAuthority('ORDER_CANCEL_OWN')")
    public ResponseEntity<ApiResult<OrderData>> cancel(
            @AuthenticationPrincipal UserPrincipal principal,
            @PathVariable Long id
    ) {
        return ResponseEntity.ok(ApiResult.ok(cancelOrderUseCase.execute(id, principal.id())));
    }

    @PatchMapping("/{id}/status")
    @PreAuthorize("hasAuthority('ORDER_UPDATE_STATUS')")
    public ResponseEntity<ApiResult<OrderData>> updateStatus(
            @PathVariable Long id,
            @Valid @RequestBody UpdateOrderStatusRequest req
    ) {
        return ResponseEntity.ok(ApiResult.ok(updateOrderStatusUseCase.execute(id, req.status())));
    }
}
