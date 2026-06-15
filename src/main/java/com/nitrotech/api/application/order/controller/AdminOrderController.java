package com.nitrotech.api.application.order.controller;

import com.nitrotech.api.domain.order.dto.OrderData;
import com.nitrotech.api.domain.order.repository.OrderRepository;
import com.nitrotech.api.domain.shipping.dto.OrderShipmentData;
import com.nitrotech.api.domain.shipping.dto.ShipmentData;
import com.nitrotech.api.domain.shipping.usecase.CreateShipmentUseCase;
import com.nitrotech.api.domain.shipping.usecase.GetOrderShipmentUseCase;
import com.nitrotech.api.shared.exception.NotFoundException;
import com.nitrotech.api.shared.response.ApiResult;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/admin/orders")
@RequiredArgsConstructor
public class AdminOrderController {

    private final OrderRepository orderRepository;
    private final CreateShipmentUseCase createShipmentUseCase;
    private final GetOrderShipmentUseCase getOrderShipmentUseCase;

    @GetMapping("/{id}")
    @PreAuthorize("hasAuthority('ORDER_READ_ALL')")
    public ResponseEntity<ApiResult<OrderData>> get(@PathVariable Long id) {
        OrderData order = orderRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("ORDER_NOT_FOUND", "Order not found"));
        return ResponseEntity.ok(ApiResult.ok(order));
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
