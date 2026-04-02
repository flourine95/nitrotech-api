package com.nitrotech.api.domain.inventory.usecase;

import com.nitrotech.api.domain.inventory.dto.InventoryData;
import com.nitrotech.api.domain.inventory.repository.InventoryRepository;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class GetInventoryUseCase {

    private final InventoryRepository inventoryRepository;

    public GetInventoryUseCase(InventoryRepository inventoryRepository) {
        this.inventoryRepository = inventoryRepository;
    }

    public InventoryData execute(Long variantId) {
        return inventoryRepository.getOrCreate(variantId);
    }

    public List<InventoryData> executeLowStock() {
        return inventoryRepository.findLowStock();
    }
}
