package com.nitrotech.api.domain.product.usecase;

import com.nitrotech.api.domain.product.exception.VariantSkuExistsException;

import com.nitrotech.api.domain.product.exception.ProductSlugExistsException;

import com.nitrotech.api.domain.brand.exception.BrandNotFoundException;

import com.nitrotech.api.domain.category.exception.CategoryNotFoundException;

import com.nitrotech.api.domain.audit.dto.AuditLogCommand;
import com.nitrotech.api.domain.audit.AuditAction;
import com.nitrotech.api.domain.audit.AuditResourceType;
import com.nitrotech.api.domain.audit.service.AuditLogService;
import com.nitrotech.api.domain.brand.repository.BrandRepository;
import com.nitrotech.api.domain.category.repository.CategoryRepository;
import com.nitrotech.api.domain.product.dto.CreateProductCommand;
import com.nitrotech.api.domain.product.dto.ProductData;
import com.nitrotech.api.domain.product.repository.ProductRepository;
import com.nitrotech.api.shared.exception.ConflictException;
import com.nitrotech.api.shared.exception.NotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class CreateProductUseCase {

    private final ProductRepository productRepository;
    private final CategoryRepository categoryRepository;
    private final BrandRepository brandRepository;
    private final AuditLogService auditLogService;

    @Transactional
    public ProductData execute(CreateProductCommand command) {
        if (!categoryRepository.existsById(command.categoryId())) {
            throw new CategoryNotFoundException();
        }
        if (command.brandId() != null && !brandRepository.existsById(command.brandId())) {
            throw new BrandNotFoundException();
        }
        if (productRepository.existsNotDeletedBySlug(command.slug())) {
            throw new ProductSlugExistsException();
        }
        if (command.variants() != null) {
            command.variants().forEach(v -> {
                if (productRepository.existsBySku(v.sku())) {
                    throw new VariantSkuExistsException(v.sku());
                }
            });
        }
        ProductData product = productRepository.create(command);
        auditLogService.record(AuditLogCommand.success(
                AuditAction.PRODUCT_CREATED,
                AuditResourceType.PRODUCT,
                product.id(),
                null,
                ProductAuditPayload.snapshot(product),
                null
        ));
        return product;
    }
}
