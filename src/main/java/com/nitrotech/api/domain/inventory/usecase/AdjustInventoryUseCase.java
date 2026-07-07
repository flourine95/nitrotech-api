package com.nitrotech.api.domain.inventory.usecase;

import com.nitrotech.api.domain.audit.AuditAction;
import com.nitrotech.api.domain.audit.AuditResourceType;
import com.nitrotech.api.domain.audit.dto.AuditLogCommand;
import com.nitrotech.api.domain.audit.service.AuditLogService;
import com.nitrotech.api.domain.inventory.dto.InventoryData;
import com.nitrotech.api.domain.inventory.exception.InsufficientStockException;
import com.nitrotech.api.domain.inventory.exception.InvalidInventoryQuantityException;
import com.nitrotech.api.domain.inventory.repository.InventoryRepository;
import com.nitrotech.api.domain.notification.dto.NotificationEvent;
import com.nitrotech.api.domain.notification.service.NotificationPublisher;
import com.nitrotech.api.domain.product.dto.ProductVariantData;
import com.nitrotech.api.domain.product.exception.VariantNotFoundException;
import com.nitrotech.api.domain.product.repository.ProductRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Map;

@Service
@RequiredArgsConstructor
public class AdjustInventoryUseCase {

    private final InventoryRepository inventoryRepository;
    private final ProductRepository productRepository;
    private final NotificationPublisher notificationPublisher;
    private final AuditLogService auditLogService;

    @Transactional
    public InventoryData adjust(Long variantId, int delta) {
        ProductVariantData variant = getVariant(variantId);
        int current = inventoryRepository.getQuantity(variantId);
        if (current + delta < 0) {
            throw new InsufficientStockException(current);
        }
        InventoryData data = inventoryRepository.adjust(variantId, delta);
        recordInventoryAudit(variant, current, data.quantity(), data.lowStockThreshold(), "adjust", delta);
        notifyIfNewLowStock(current, data);
        return data;
    }

    @Transactional
    public InventoryData setQuantity(Long variantId, int quantity) {
        ProductVariantData variant = getVariant(variantId);
        if (quantity < 0) {
            throw new InvalidInventoryQuantityException();
        }
        int current = inventoryRepository.getQuantity(variantId);
        InventoryData data = inventoryRepository.setQuantity(variantId, quantity);
        recordInventoryAudit(variant, current, data.quantity(), data.lowStockThreshold(), "setQuantity", null);
        notifyIfNewLowStock(current, data);
        return data;
    }

    @Transactional
    public InventoryData setThreshold(Long variantId, int threshold) {
        ProductVariantData variant = getVariant(variantId);
        int current = inventoryRepository.getQuantity(variantId);
        InventoryData data = inventoryRepository.setThreshold(variantId, threshold);
        recordInventoryAudit(variant, current, data.quantity(), data.lowStockThreshold(), "setThreshold", null);
        return data;
    }

    private ProductVariantData getVariant(Long variantId) {
        return productRepository.findVariantById(variantId)
                .orElseThrow(() -> new VariantNotFoundException());
    }

    private void recordInventoryAudit(
            ProductVariantData variant,
            int beforeQuantity,
            int afterQuantity,
            int lowStockThreshold,
            String operation,
            Integer delta
    ) {
        auditLogService.record(AuditLogCommand.success(
                AuditAction.PRODUCT_INVENTORY_UPDATED,
                AuditResourceType.PRODUCT,
                variant.productId(),
                Map.of("quantity", beforeQuantity),
                Map.of("quantity", afterQuantity, "lowStockThreshold", lowStockThreshold),
                delta == null
                        ? Map.of("variantId", variant.id(), "sku", variant.sku(), "operation", operation)
                        : Map.of("variantId", variant.id(), "sku", variant.sku(), "operation", operation, "delta", delta)
        ));
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
