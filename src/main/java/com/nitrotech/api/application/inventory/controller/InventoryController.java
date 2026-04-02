package com.nitrotech.api.application.inventory.controller;

import com.nitrotech.api.application.inventory.request.AdjustInventoryRequest;
import com.nitrotech.api.application.inventory.request.SetInventoryRequest;
import com.nitrotech.api.domain.inventory.dto.InventoryData;
import com.nitrotech.api.domain.inventory.usecase.AdjustInventoryUseCase;
import com.nitrotech.api.domain.inventory.usecase.GetInventoryUseCase;
import com.nitrotech.api.shared.response.ApiResponse;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/inventory")
public class InventoryController {

    private final GetInventoryUseCase getInventoryUseCase;
    private final AdjustInventoryUseCase adjustInventoryUseCase;

    public InventoryController(GetInventoryUseCase getInventoryUseCase,
                                AdjustInventoryUseCase adjustInventoryUseCase) {
        this.getInventoryUseCase = getInventoryUseCase;
        this.adjustInventoryUseCase = adjustInventoryUseCase;
    }

    @GetMapping("/variants/{variantId}")
    public ResponseEntity<ApiResponse<InventoryData>> get(@PathVariable Long variantId) {
        return ResponseEntity.ok(ApiResponse.ok(getInventoryUseCase.execute(variantId)));
    }

    @GetMapping("/low-stock")
    public ResponseEntity<ApiResponse<List<InventoryData>>> lowStock() {
        return ResponseEntity.ok(ApiResponse.ok(getInventoryUseCase.executeLowStock()));
    }

    // Điều chỉnh tương đối: +10 nhập kho, -5 xuất kho
    @PatchMapping("/variants/{variantId}/adjust")
    public ResponseEntity<ApiResponse<InventoryData>> adjust(
            @PathVariable Long variantId,
            @Valid @RequestBody AdjustInventoryRequest req
    ) {
        return ResponseEntity.ok(ApiResponse.ok(adjustInventoryUseCase.adjust(variantId, req.delta())));
    }

    // Set tuyệt đối: quantity và threshold
    @PutMapping("/variants/{variantId}")
    public ResponseEntity<ApiResponse<InventoryData>> set(
            @PathVariable Long variantId,
            @Valid @RequestBody SetInventoryRequest req
    ) {
        InventoryData data = adjustInventoryUseCase.setQuantity(variantId, req.quantity());
        if (req.lowStockThreshold() != null) {
            data = adjustInventoryUseCase.setThreshold(variantId, req.lowStockThreshold());
        }
        return ResponseEntity.ok(ApiResponse.ok(data));
    }
}
