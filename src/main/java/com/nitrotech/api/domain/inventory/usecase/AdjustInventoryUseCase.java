package com.nitrotech.api.domain.inventory.usecase;

import com.nitrotech.api.domain.inventory.dto.InventoryData;
import com.nitrotech.api.domain.inventory.repository.InventoryRepository;
import com.nitrotech.api.domain.product.repository.ProductRepository;
import com.nitrotech.api.shared.exception.DomainException;
import com.nitrotech.api.shared.exception.NotFoundException;
import org.springframework.stereotype.Service;

@Service
public class AdjustInventoryUseCase {

    private final InventoryRepository inventoryRepository;
    private final ProductRepository productRepository;

    public AdjustInventoryUseCase(InventoryRepository inventoryRepository,
                                   ProductRepository productRepository) {
        this.inventoryRepository = inventoryRepository;
        this.productRepository = productRepository;
    }

    public InventoryData adjust(Long variantId, int delta) {
        if (!productRepository.existsVariantById(variantId)) {
            throw new NotFoundException("VARIANT_NOT_FOUND", "Variant not found");
        }
        int current = inventoryRepository.getQuantity(variantId);
        if (current + delta < 0) {
            throw new DomainException("INSUFFICIENT_STOCK",
                    "Insufficient stock. Available: " + current) {};
        }
        return inventoryRepository.adjust(variantId, delta);
    }

    public InventoryData setQuantity(Long variantId, int quantity) {
        if (!productRepository.existsVariantById(variantId)) {
            throw new NotFoundException("VARIANT_NOT_FOUND", "Variant not found");
        }
        if (quantity < 0) {
            throw new DomainException("INVALID_QUANTITY", "Quantity cannot be negative") {};
        }
        return inventoryRepository.setQuantity(variantId, quantity);
    }

    public InventoryData setThreshold(Long variantId, int threshold) {
        if (!productRepository.existsVariantById(variantId)) {
            throw new NotFoundException("VARIANT_NOT_FOUND", "Variant not found");
        }
        return inventoryRepository.setThreshold(variantId, threshold);
    }
}
