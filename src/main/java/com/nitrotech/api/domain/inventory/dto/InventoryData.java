package com.nitrotech.api.domain.inventory.dto;

import java.time.LocalDateTime;

public record InventoryData(
        Long id,
        Long variantId,
        String variantSku,
        String variantName,
        int quantity,
        int lowStockThreshold,
        boolean lowStock,
        LocalDateTime updatedAt
) {}
