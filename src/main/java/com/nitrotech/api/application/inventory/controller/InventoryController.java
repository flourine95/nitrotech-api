package com.nitrotech.api.application.inventory.controller;

import com.nitrotech.api.application.inventory.request.AdjustInventoryRequest;
import com.nitrotech.api.application.inventory.request.SetInventoryRequest;
import com.nitrotech.api.domain.inventory.dto.InventoryData;
import com.nitrotech.api.domain.inventory.usecase.AdjustInventoryUseCase;
import com.nitrotech.api.domain.inventory.usecase.GetInventoryUseCase;
import com.nitrotech.api.shared.response.ApiResult;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/inventory")
@RequiredArgsConstructor
public class InventoryController {

    private final GetInventoryUseCase getInventoryUseCase;
    private final AdjustInventoryUseCase adjustInventoryUseCase;

    @GetMapping("/variants/{variantId}")
    public ResponseEntity<ApiResult<InventoryData>> get(
            @PathVariable Long variantId
    ) {
        return ResponseEntity.ok(ApiResult.ok(getInventoryUseCase.execute(variantId)));
    }

    @GetMapping("/low-stock")
    public ResponseEntity<ApiResult<List<InventoryData>>> lowStock() {
        return ResponseEntity.ok(ApiResult.ok(getInventoryUseCase.executeLowStock()));
    }

    @PatchMapping("/variants/{variantId}/adjust")
    public ResponseEntity<ApiResult<InventoryData>> adjust(
            @PathVariable Long variantId,
            @Valid @RequestBody AdjustInventoryRequest req
    ) {
        return ResponseEntity.ok(ApiResult.ok(adjustInventoryUseCase.adjust(variantId, req.delta())));
    }

    @PutMapping("/variants/{variantId}")
    public ResponseEntity<ApiResult<InventoryData>> set(
            @PathVariable Long variantId,
            @Valid @RequestBody SetInventoryRequest req
    ) {
        InventoryData data = adjustInventoryUseCase.setQuantity(variantId, req.quantity());
        if (req.lowStockThreshold() != null) {
            data = adjustInventoryUseCase.setThreshold(variantId, req.lowStockThreshold());
        }
        return ResponseEntity.ok(ApiResult.ok(data));
    }
}