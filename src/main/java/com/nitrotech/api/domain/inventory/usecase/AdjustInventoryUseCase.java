package com.nitrotech.api.domain.inventory.usecase;

import com.nitrotech.api.domain.inventory.dto.InventoryData;
import com.nitrotech.api.domain.inventory.exception.InsufficientStockException;
import com.nitrotech.api.domain.inventory.exception.InvalidInventoryQuantityException;
import com.nitrotech.api.domain.inventory.repository.InventoryRepository;
import com.nitrotech.api.domain.notification.dto.NotificationEvent;
import com.nitrotech.api.domain.notification.service.NotificationPublisher;
import com.nitrotech.api.domain.product.exception.VariantNotFoundException;
import com.nitrotech.api.domain.product.repository.ProductRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class AdjustInventoryUseCase {

    private final InventoryRepository inventoryRepository;
    private final ProductRepository productRepository;
    private final NotificationPublisher notificationPublisher;

    public InventoryData adjust(Long variantId, int delta) {
        if (!productRepository.existsVariantById(variantId)) {
            throw new VariantNotFoundException();
        }
        int current = inventoryRepository.getQuantity(variantId);
        if (current + delta < 0) {
            throw new InsufficientStockException(current);
        }
        InventoryData data = inventoryRepository.adjust(variantId, delta);
        notifyIfNewLowStock(current, data);
        return data;
    }

    public InventoryData setQuantity(Long variantId, int quantity) {
        if (!productRepository.existsVariantById(variantId)) {
            throw new VariantNotFoundException();
        }
        if (quantity < 0) {
            throw new InvalidInventoryQuantityException();
        }
        int current = inventoryRepository.getQuantity(variantId);
        InventoryData data = inventoryRepository.setQuantity(variantId, quantity);
        notifyIfNewLowStock(current, data);
        return data;
    }

    public InventoryData setThreshold(Long variantId, int threshold) {
        if (!productRepository.existsVariantById(variantId)) {
            throw new VariantNotFoundException();
        }
        return inventoryRepository.setThreshold(variantId, threshold);
    }

    private void notifyIfNewLowStock(int previousQuantity, InventoryData data) {
        if (previousQuantity <= data.lowStockThreshold() || !data.lowStock()) {
            return;
        }
        notificationPublisher.publish(new NotificationEvent(
                null,
                "LOW_STOCK",
                "Cảnh báo tồn kho",
                data.variantName() + " chỉ còn " + data.quantity() + " sản phẩm.",
                "/dashboard/products",
                null,
                null,
                "INVENTORY_MANAGE"
        ));
    }
}
