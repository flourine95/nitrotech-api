package com.nitrotech.api.domain.inventory.repository;

import com.nitrotech.api.domain.inventory.dto.InventoryData;

import java.util.List;
import java.util.Optional;

public interface InventoryRepository {
    InventoryData getOrCreate(Long variantId);
    InventoryData adjust(Long variantId, int delta); // delta dương = nhập, âm = xuất
    InventoryData setQuantity(Long variantId, int quantity);
    InventoryData setThreshold(Long variantId, int threshold);
    Optional<InventoryData> findByVariantId(Long variantId);
    List<InventoryData> findLowStock();
    int getQuantity(Long variantId);
    boolean hasSufficientStock(Long variantId, int required);
}
