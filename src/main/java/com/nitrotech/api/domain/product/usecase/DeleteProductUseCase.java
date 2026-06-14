package com.nitrotech.api.domain.product.usecase;

import com.nitrotech.api.domain.audit.dto.AuditLogCommand;
import com.nitrotech.api.domain.audit.service.AuditLogService;
import com.nitrotech.api.domain.product.dto.ProductData;
import com.nitrotech.api.domain.product.repository.ProductRepository;
import com.nitrotech.api.shared.exception.NotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Map;

@Service
@RequiredArgsConstructor
public class DeleteProductUseCase {

    private final ProductRepository productRepository;
    private final AuditLogService auditLogService;

    @Transactional
    public void execute(Long id) {
        ProductData product = productRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("PRODUCT_NOT_FOUND", "Product not found"));
        productRepository.softDelete(id);
        auditLogService.record(AuditLogCommand.success(
                "PRODUCT_DELETED",
                "PRODUCT",
                id,
                Map.of("name", product.name(), "slug", product.slug()),
                null,
                null
        ));
    }
}
