package com.nitrotech.api.domain.product.usecase;

import com.nitrotech.api.domain.brand.repository.BrandRepository;
import com.nitrotech.api.domain.category.repository.CategoryRepository;
import com.nitrotech.api.domain.product.dto.ProductData;
import com.nitrotech.api.domain.product.dto.UpdateProductCommand;
import com.nitrotech.api.domain.product.repository.ProductRepository;
import com.nitrotech.api.shared.exception.ConflictException;
import com.nitrotech.api.shared.exception.NotFoundException;
import org.springframework.stereotype.Service;

@Service
public class UpdateProductUseCase {

    private final ProductRepository productRepository;
    private final CategoryRepository categoryRepository;
    private final BrandRepository brandRepository;

    public UpdateProductUseCase(ProductRepository productRepository,
                                 CategoryRepository categoryRepository,
                                 BrandRepository brandRepository) {
        this.productRepository = productRepository;
        this.categoryRepository = categoryRepository;
        this.brandRepository = brandRepository;
    }

    public ProductData execute(UpdateProductCommand command) {
        if (!productRepository.existsById(command.id())) {
            throw new NotFoundException("PRODUCT_NOT_FOUND", "Product not found");
        }
        if (command.categoryId() != null && !categoryRepository.existsById(command.categoryId())) {
            throw new NotFoundException("CATEGORY_NOT_FOUND", "Category not found");
        }
        if (command.brandId() != null && !brandRepository.existsById(command.brandId())) {
            throw new NotFoundException("BRAND_NOT_FOUND", "Brand not found");
        }
        if (command.slug() != null && productRepository.existsBySlugAndIdNot(command.slug(), command.id())) {
            throw new ConflictException("PRODUCT_SLUG_EXISTS", "Slug already exists");
        }
        return productRepository.update(command);
    }
}
