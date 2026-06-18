package com.nitrotech.api.application.order.controller;

import com.nitrotech.api.application.order.request.OrderListRequest;
import com.nitrotech.api.application.order.request.UpdateOrderStatusRequest;
import com.nitrotech.api.domain.order.dto.OrderData;
import com.nitrotech.api.domain.order.dto.OrderFacetsData;
import com.nitrotech.api.domain.order.dto.OrderListItemData;
import com.nitrotech.api.domain.order.usecase.GetOrderFacetsUseCase;
import com.nitrotech.api.domain.order.usecase.GetOrderUseCase;
import com.nitrotech.api.domain.order.usecase.GetOrdersUseCase;
import com.nitrotech.api.domain.order.usecase.UpdateOrderStatusUseCase;
import com.nitrotech.api.domain.shipping.dto.OrderShipmentData;
import com.nitrotech.api.domain.shipping.dto.ShipmentData;
import com.nitrotech.api.domain.shipping.usecase.CreateShipmentUseCase;
import com.nitrotech.api.domain.shipping.usecase.GetOrderShipmentUseCase;
import com.nitrotech.api.shared.response.ApiResult;
import com.nitrotech.api.shared.validation.ValidSortFields;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/admin/orders")
@RequiredArgsConstructor
@Validated
public class AdminOrderController {

    private final GetOrdersUseCase getOrdersUseCase;
    private final GetOrderUseCase getOrderUseCase;
    private final GetOrderFacetsUseCase getOrderFacetsUseCase;
    private final UpdateOrderStatusUseCase updateOrderStatusUseCase;
    private final CreateShipmentUseCase createShipmentUseCase;
    private final GetOrderShipmentUseCase getOrderShipmentUseCase;

    @GetMapping
    @PreAuthorize("hasAuthority('ORDER_READ_ALL')")
    public ResponseEntity<ApiResult<List<OrderListItemData>>> list(
            @Valid @ModelAttribute OrderListRequest filter,
            @PageableDefault(size = 20, sort = "createdAt", direction = Sort.Direction.DESC)
            @ValidSortFields({"id", "userId", "status", "paymentMethod", "finalAmount", "createdAt", "updatedAt"})
            Pageable pageable
    ) {
        var page = getOrdersUseCase.execute(filter.toFilter(null), pageable);
        return ResponseEntity.ok(ApiResult.paged(page));
    }

    @GetMapping("/facets")
    @PreAuthorize("hasAuthority('ORDER_READ_ALL')")
    public ResponseEntity<ApiResult<OrderFacetsData>> facets(
            @Valid @ModelAttribute OrderListRequest filter
    ) {
        return ResponseEntity.ok(ApiResult.ok(getOrderFacetsUseCase.execute(filter.toFilter(null))));
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAuthority('ORDER_READ_ALL')")
    public ResponseEntity<ApiResult<OrderData>> get(@PathVariable Long id) {
        return ResponseEntity.ok(ApiResult.ok(getOrderUseCase.executeById(id)));
    }

    @PatchMapping("/{id}/status")
    @PreAuthorize("hasAuthority('ORDER_UPDATE_STATUS')")
    public ResponseEntity<ApiResult<OrderData>> updateStatus(
            @PathVariable Long id,
            @Valid @RequestBody UpdateOrderStatusRequest req
    ) {
        return ResponseEntity.ok(ApiResult.ok(updateOrderStatusUseCase.execute(id, req.status())));
    }

    @GetMapping("/{id}/shipment")
    @PreAuthorize("hasAuthority('SHIPMENT_READ')")
    public ResponseEntity<ApiResult<OrderShipmentData>> getShipment(@PathVariable Long id) {
        return ResponseEntity.ok(ApiResult.ok(getOrderShipmentUseCase.execute(id)));
    }

    @PostMapping("/{id}/shipment")
    @PreAuthorize("hasAuthority('SHIPMENT_CREATE')")
    public ResponseEntity<ApiResult<ShipmentData>> createShipment(
            @PathVariable Long id,
            @RequestParam(required = false) String provider
    ) {
        ShipmentData shipment = createShipmentUseCase.execute(id, provider);
        return ResponseEntity.ok(ApiResult.ok(shipment));
    }
}
