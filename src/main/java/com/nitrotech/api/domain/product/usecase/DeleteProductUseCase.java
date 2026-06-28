package com.nitrotech.api.domain.product.usecase;

import com.nitrotech.api.domain.audit.AuditAction;
import com.nitrotech.api.domain.audit.AuditResourceType;
import com.nitrotech.api.domain.audit.dto.AuditLogCommand;
import com.nitrotech.api.domain.audit.service.AuditLogService;
import com.nitrotech.api.domain.product.dto.ProductData;
import com.nitrotech.api.domain.product.exception.ProductNotFoundException;
import com.nitrotech.api.domain.product.repository.ProductRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class DeleteProductUseCase {

    private final ProductRepository productRepository;
    private final AuditLogService auditLogService;

    @Transactional
    public void execute(Long id) {
        ProductData product = productRepository.findNotDeletedById(id)
                .orElseThrow(() -> new ProductNotFoundException());
        productRepository.softDelete(id);
        auditLogService.record(AuditLogCommand.success(
                AuditAction.PRODUCT_DELETED,
                AuditResourceType.PRODUCT,
                id,
                ProductAuditPayload.snapshot(product),
                null,
                null
        ));
    }
}
