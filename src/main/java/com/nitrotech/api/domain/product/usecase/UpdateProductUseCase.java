package com.nitrotech.api.domain.product.usecase;

import com.nitrotech.api.domain.audit.dto.AuditLogCommand;
import com.nitrotech.api.domain.audit.dto.AuditAction;
import com.nitrotech.api.domain.audit.dto.AuditResourceType;
import com.nitrotech.api.domain.audit.service.AuditLogService;
import com.nitrotech.api.domain.brand.repository.BrandRepository;
import com.nitrotech.api.domain.category.repository.CategoryRepository;
import com.nitrotech.api.domain.product.dto.ProductData;
import com.nitrotech.api.domain.product.dto.UpdateProductCommand;
import com.nitrotech.api.domain.product.repository.ProductRepository;
import com.nitrotech.api.shared.exception.ConflictException;
import com.nitrotech.api.shared.exception.NotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Map;

@Service
@RequiredArgsConstructor
public class UpdateProductUseCase {

    private final ProductRepository productRepository;
    private final CategoryRepository categoryRepository;
    private final BrandRepository brandRepository;
    private final AuditLogService auditLogService;

    @Transactional
    public ProductData execute(UpdateProductCommand command) {
        ProductData before = productRepository.findById(command.id())
                .orElseThrow(() -> new NotFoundException("PRODUCT_NOT_FOUND", "Product not found"));

        if (command.categoryId() != null && !categoryRepository.existsById(command.categoryId())) {
            throw new NotFoundException("CATEGORY_NOT_FOUND", "Category not found");
        }
        if (command.brandId() != null && !brandRepository.existsById(command.brandId())) {
            throw new NotFoundException("BRAND_NOT_FOUND", "Brand not found");
        }
        if (command.slug() != null && productRepository.existsBySlugAndIdNot(command.slug(), command.id())) {
            throw new ConflictException("PRODUCT_SLUG_EXISTS", "Slug already exists");
        }
        ProductData after = productRepository.update(command);
        auditLogService.record(AuditLogCommand.success(
                AuditAction.PRODUCT_UPDATED,
                AuditResourceType.PRODUCT,
                command.id(),
                Map.of("name", before.name(), "slug", before.slug(), "active", before.active()),
                Map.of("name", after.name(), "slug", after.slug(), "active", after.active()),
                null
        ));
        return after;
    }
}
