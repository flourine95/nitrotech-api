package com.nitrotech.api.application.order.controller;

import com.nitrotech.api.domain.shipping.dto.ShipmentData;
import com.nitrotech.api.domain.shipping.usecase.CreateShipmentUseCase;
import com.nitrotech.api.shared.response.ApiResult;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/admin/orders")
@RequiredArgsConstructor
public class AdminOrderController {

    private final CreateShipmentUseCase createShipmentUseCase;

    @PostMapping("/{id}/shipment")
    public ResponseEntity<ApiResult<ShipmentData>> createShipment(
            @PathVariable Long id,
            @RequestParam(required = false) String provider
    ) {
        ShipmentData shipment = createShipmentUseCase.execute(id, provider);
        return ResponseEntity.ok(ApiResult.ok(shipment));
    }
}
