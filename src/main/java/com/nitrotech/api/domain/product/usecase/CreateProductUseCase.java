package com.nitrotech.api.domain.product.usecase;

import com.nitrotech.api.domain.brand.repository.BrandRepository;
import com.nitrotech.api.domain.category.repository.CategoryRepository;
import com.nitrotech.api.domain.product.dto.CreateProductCommand;
import com.nitrotech.api.domain.product.dto.ProductData;
import com.nitrotech.api.domain.product.repository.ProductRepository;
import com.nitrotech.api.shared.exception.ConflictException;
import com.nitrotech.api.shared.exception.NotFoundException;
import org.springframework.stereotype.Service;

@Service
public class CreateProductUseCase {

    private final ProductRepository productRepository;
    private final CategoryRepository categoryRepository;
    private final BrandRepository brandRepository;

    public CreateProductUseCase(ProductRepository productRepository,
                                 CategoryRepository categoryRepository,
                                 BrandRepository brandRepository) {
        this.productRepository = productRepository;
        this.categoryRepository = categoryRepository;
        this.brandRepository = brandRepository;
    }

    public ProductData execute(CreateProductCommand command) {
        if (!categoryRepository.existsById(command.categoryId())) {
            throw new NotFoundException("CATEGORY_NOT_FOUND", "Category not found");
        }
        if (command.brandId() != null && !brandRepository.existsById(command.brandId())) {
            throw new NotFoundException("BRAND_NOT_FOUND", "Brand not found");
        }
        if (productRepository.existsBySlug(command.slug())) {
            throw new ConflictException("PRODUCT_SLUG_EXISTS", "Slug already exists");
        }
        if (command.variants() != null) {
            command.variants().forEach(v -> {
                if (productRepository.existsBySku(v.sku())) {
                    throw new ConflictException("VARIANT_SKU_EXISTS", "SKU already exists: " + v.sku());
                }
            });
        }
        return productRepository.create(command);
    }
}
