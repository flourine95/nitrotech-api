package com.nitrotech.api.domain.product.usecase;

import com.nitrotech.api.domain.audit.AuditAction;
import com.nitrotech.api.domain.audit.AuditResourceType;
import com.nitrotech.api.domain.audit.dto.AuditLogCommand;
import com.nitrotech.api.domain.audit.service.AuditLogService;
import com.nitrotech.api.domain.product.dto.ProductVariantData;
import com.nitrotech.api.domain.product.dto.UpdateVariantCommand;
import com.nitrotech.api.domain.product.exception.VariantNotFoundException;
import com.nitrotech.api.domain.product.exception.VariantSkuExistsException;
import com.nitrotech.api.domain.product.repository.ProductRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;

@Service
@RequiredArgsConstructor
public class UpdateVariantUseCase {

    private final ProductRepository productRepository;
    private final AuditLogService auditLogService;

    @Transactional
    public ProductVariantData execute(UpdateVariantCommand command) {
        ProductVariantData before = productRepository.findVariantById(command.id())
                .orElseThrow(() -> new VariantNotFoundException());
        if (command.sku() != null && productRepository.existsBySkuAndIdNot(command.sku(), command.id())) {
            throw new VariantSkuExistsException();
        }
        ProductVariantData after = productRepository.updateVariant(command);
        Map<String, Object> beforeData = new LinkedHashMap<>();
        Map<String, Object> afterData = new LinkedHashMap<>();
        addIfChanged(beforeData, afterData, "sku", before.sku(), after.sku());
        addIfChanged(beforeData, afterData, "name", before.name(), after.name());
        addIfChanged(beforeData, afterData, "price", before.price(), after.price());
        addIfChanged(beforeData, afterData, "attributes", before.attributes(), after.attributes());
        addIfChanged(beforeData, afterData, "active", before.active(), after.active());
        addIfChanged(beforeData, afterData, "weightGrams", before.weightGrams(), after.weightGrams());
        addIfChanged(beforeData, afterData, "lengthCm", before.lengthCm(), after.lengthCm());
        addIfChanged(beforeData, afterData, "widthCm", before.widthCm(), after.widthCm());
        addIfChanged(beforeData, afterData, "heightCm", before.heightCm(), after.heightCm());
        auditLogService.record(AuditLogCommand.success(
                AuditAction.PRODUCT_VARIANT_UPDATED,
                AuditResourceType.PRODUCT,
                after.productId(),
                beforeData,
                afterData,
                Map.of("variantId", after.id())
        ));
        return after;
    }

    private void addIfChanged(
            Map<String, Object> beforeData,
            Map<String, Object> afterData,
            String key,
            Object before,
            Object after
    ) {
        if (!Objects.equals(before, after)) {
            beforeData.put(key, before);
            afterData.put(key, after);
        }
    }
}
