package com.nitrotech.api.domain.product.usecase;

import com.nitrotech.api.domain.audit.AuditAction;
import com.nitrotech.api.domain.audit.AuditResourceType;
import com.nitrotech.api.domain.audit.dto.AuditLogCommand;
import com.nitrotech.api.domain.audit.service.AuditLogService;
import com.nitrotech.api.domain.product.dto.ProductVariantData;
import com.nitrotech.api.domain.product.exception.VariantNotFoundException;
import com.nitrotech.api.domain.product.repository.ProductRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.LinkedHashMap;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class DeleteVariantUseCase {

    private final ProductRepository productRepository;
    private final AuditLogService auditLogService;

    @Transactional
    public void execute(Long id) {
        ProductVariantData variant = productRepository.findVariantById(id)
                .orElseThrow(() -> new VariantNotFoundException());
        productRepository.softDeleteVariant(id);
        auditLogService.record(AuditLogCommand.success(
                AuditAction.PRODUCT_VARIANT_DELETED,
                AuditResourceType.PRODUCT,
                variant.productId(),
                variantPayload(variant),
                null,
                Map.of("variantId", variant.id())
        ));
    }

    private Map<String, Object> variantPayload(ProductVariantData variant) {
        Map<String, Object> data = new LinkedHashMap<>();
        data.put("variantId", variant.id());
        data.put("sku", variant.sku());
        data.put("name", variant.name());
        data.put("price", variant.price());
        data.put("attributes", variant.attributes());
        data.put("active", variant.active());
        return data;
    }
}
