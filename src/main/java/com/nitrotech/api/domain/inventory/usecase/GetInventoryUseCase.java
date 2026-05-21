package com.nitrotech.api.domain.inventory.usecase;

import com.nitrotech.api.domain.inventory.dto.InventoryData;
import com.nitrotech.api.domain.inventory.repository.InventoryRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class GetInventoryUseCase {

    private final InventoryRepository inventoryRepository;

    public InventoryData execute(Long variantId) {
        return inventoryRepository.getOrCreate(variantId);
    }

    public List<InventoryData> executeLowStock() {
        return inventoryRepository.findLowStock();
    }
}
