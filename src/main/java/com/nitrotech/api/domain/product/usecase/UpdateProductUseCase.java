package com.nitrotech.api.domain.product.usecase;

import com.nitrotech.api.domain.audit.AuditAction;
import com.nitrotech.api.domain.audit.AuditResourceType;
import com.nitrotech.api.domain.audit.dto.AuditLogCommand;
import com.nitrotech.api.domain.audit.service.AuditLogService;
import com.nitrotech.api.domain.brand.exception.BrandNotFoundException;
import com.nitrotech.api.domain.brand.repository.BrandRepository;
import com.nitrotech.api.domain.category.exception.CategoryNotFoundException;
import com.nitrotech.api.domain.category.repository.CategoryRepository;
import com.nitrotech.api.domain.product.dto.ProductData;
import com.nitrotech.api.domain.product.dto.UpdateProductCommand;
import com.nitrotech.api.domain.product.exception.ProductNotFoundException;
import com.nitrotech.api.domain.product.exception.ProductSlugExistsException;
import com.nitrotech.api.domain.product.repository.ProductRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class UpdateProductUseCase {

    private final ProductRepository productRepository;
    private final CategoryRepository categoryRepository;
    private final BrandRepository brandRepository;
    private final AuditLogService auditLogService;

    @Transactional
    public ProductData execute(UpdateProductCommand command) {
        ProductData before = productRepository.findNotDeletedById(command.id())
                .orElseThrow(() -> new ProductNotFoundException());

        if (command.categoryId() != null && !categoryRepository.existsById(command.categoryId())) {
            throw new CategoryNotFoundException();
        }
        if (command.brandId() != null && !brandRepository.existsById(command.brandId())) {
            throw new BrandNotFoundException();
        }
        if (command.slug() != null && productRepository.existsNotDeletedBySlugAndIdNot(command.slug(), command.id())) {
            throw new ProductSlugExistsException();
        }
        ProductData after = productRepository.update(command);
        ProductAuditPayload.ProductDelta delta = ProductAuditPayload.delta(before, after);
        auditLogService.record(AuditLogCommand.success(
                AuditAction.PRODUCT_UPDATED,
                AuditResourceType.PRODUCT,
                command.id(),
                delta.beforeData(),
                delta.afterData(),
                null
        ));
        return after;
    }
}
