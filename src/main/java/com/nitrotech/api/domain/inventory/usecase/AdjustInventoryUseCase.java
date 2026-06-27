package com.nitrotech.api.domain.inventory.usecase;

import com.nitrotech.api.domain.product.exception.VariantNotFoundException;

import com.nitrotech.api.domain.inventory.dto.InventoryData;
import com.nitrotech.api.domain.inventory.exception.InsufficientStockException;
import com.nitrotech.api.domain.inventory.exception.InvalidInventoryQuantityException;
import com.nitrotech.api.domain.inventory.repository.InventoryRepository;
import com.nitrotech.api.domain.product.repository.ProductRepository;
import com.nitrotech.api.shared.exception.NotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class AdjustInventoryUseCase {

    private final InventoryRepository inventoryRepository;
    private final ProductRepository productRepository;

    public InventoryData adjust(Long variantId, int delta) {
        if (!productRepository.existsVariantById(variantId)) {
            throw new VariantNotFoundException();
        }
        int current = inventoryRepository.getQuantity(variantId);
        if (current + delta < 0) {
            throw new InsufficientStockException(current);
        }
        return inventoryRepository.adjust(variantId, delta);
    }

    public InventoryData setQuantity(Long variantId, int quantity) {
        if (!productRepository.existsVariantById(variantId)) {
            throw new VariantNotFoundException();
        }
        if (quantity < 0) {
            throw new InvalidInventoryQuantityException();
        }
        return inventoryRepository.setQuantity(variantId, quantity);
    }

    public InventoryData setThreshold(Long variantId, int threshold) {
        if (!productRepository.existsVariantById(variantId)) {
            throw new VariantNotFoundException();
        }
        return inventoryRepository.setThreshold(variantId, threshold);
    }
}
