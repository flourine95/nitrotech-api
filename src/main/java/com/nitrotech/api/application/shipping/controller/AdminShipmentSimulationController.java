package com.nitrotech.api.application.shipping.controller;

import com.nitrotech.api.application.shipping.request.SimulateShipmentEventRequest;
import com.nitrotech.api.domain.shipping.dto.ShipmentData;
import com.nitrotech.api.domain.shipping.usecase.SimulateShipmentEventUseCase;
import com.nitrotech.api.shared.response.ApiResult;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/admin/shipments")
@RequiredArgsConstructor
public class AdminShipmentSimulationController {

    private final SimulateShipmentEventUseCase simulateShipmentEventUseCase;

    @PostMapping("/{id}/simulation-events")
    @PreAuthorize("hasAuthority('SHIPMENT_UPDATE')")
    public ResponseEntity<ApiResult<ShipmentData>> simulate(
            @PathVariable Long id,
            @Valid @RequestBody SimulateShipmentEventRequest request
    ) {
        ShipmentData data = simulateShipmentEventUseCase.execute(
                id,
                request.status(),
                request.location(),
                request.note()
        );
        return ResponseEntity.ok(ApiResult.ok(data));
    }
}
