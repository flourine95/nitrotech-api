package com.nitrotech.api.application.inventory.controller;

import com.nitrotech.api.application.inventory.request.AdjustInventoryRequest;
import com.nitrotech.api.application.inventory.request.SetInventoryRequest;
import com.nitrotech.api.domain.inventory.dto.InventoryData;
import com.nitrotech.api.domain.inventory.usecase.AdjustInventoryUseCase;
import com.nitrotech.api.domain.inventory.usecase.GetInventoryUseCase;
import com.nitrotech.api.shared.response.ApiResult;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/inventory")
@Tag(name = "Inventory", description = "Product variant inventory management APIs")
@RequiredArgsConstructor
public class InventoryController {

    private final GetInventoryUseCase getInventoryUseCase;
    private final AdjustInventoryUseCase adjustInventoryUseCase;

    @Operation(summary = "Get inventory for variant", description = "Retrieve current stock quantity and low-stock threshold for a specific variant.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Inventory data retrieved"),
            @ApiResponse(responseCode = "404", description = "Variant not found", content = @Content(mediaType = "application/json"))
    })
    @GetMapping("/variants/{variantId}")
    public ResponseEntity<ApiResult<InventoryData>> get(
            @Parameter(description = "Variant ID") @PathVariable Long variantId
    ) {
        return ResponseEntity.ok(ApiResult.ok(getInventoryUseCase.execute(variantId)));
    }

    @Operation(summary = "List low-stock variants", description = "Get all variants whose current stock is at or below their low-stock threshold.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Low-stock variants retrieved")
    })
    @GetMapping("/low-stock")
    public ResponseEntity<ApiResult<List<InventoryData>>> lowStock() {
        return ResponseEntity.ok(ApiResult.ok(getInventoryUseCase.executeLowStock()));
    }

    @Operation(summary = "Adjust inventory (relative)", description = "Adjust stock by a relative delta. Use positive values for stock-in (+10) and negative for stock-out (-5).")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Inventory adjusted successfully"),
            @ApiResponse(responseCode = "400", description = "Invalid delta value or would result in negative stock", content = @Content(mediaType = "application/json")),
            @ApiResponse(responseCode = "404", description = "Variant not found", content = @Content(mediaType = "application/json"))
    })
    @PatchMapping("/variants/{variantId}/adjust")
    public ResponseEntity<ApiResult<InventoryData>> adjust(
            @Parameter(description = "Variant ID") @PathVariable Long variantId,
            @Valid @RequestBody AdjustInventoryRequest req
    ) {
        return ResponseEntity.ok(ApiResult.ok(adjustInventoryUseCase.adjust(variantId, req.delta())));
    }

    @Operation(summary = "Set inventory (absolute)", description = "Set the exact stock quantity and/or low-stock threshold for a variant.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Inventory set successfully"),
            @ApiResponse(responseCode = "400", description = "Invalid quantity or threshold value", content = @Content(mediaType = "application/json")),
            @ApiResponse(responseCode = "404", description = "Variant not found", content = @Content(mediaType = "application/json"))
    })
    @PutMapping("/variants/{variantId}")
    public ResponseEntity<ApiResult<InventoryData>> set(
            @Parameter(description = "Variant ID") @PathVariable Long variantId,
            @Valid @RequestBody SetInventoryRequest req
    ) {
        InventoryData data = adjustInventoryUseCase.setQuantity(variantId, req.quantity());
        if (req.lowStockThreshold() != null) {
            data = adjustInventoryUseCase.setThreshold(variantId, req.lowStockThreshold());
        }
        return ResponseEntity.ok(ApiResult.ok(data));
    }
}
