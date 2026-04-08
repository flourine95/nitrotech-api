package com.nitrotech.api.infrastructure.persistence.repository;

import com.nitrotech.api.domain.category.usecase.ProductCategoryChecker;
import org.springframework.stereotype.Component;

@Component
public class ProductCategoryCheckerImpl implements ProductCategoryChecker {

    private final ProductJpaRepository productJpa;

    public ProductCategoryCheckerImpl(ProductJpaRepository productJpa) {
        this.productJpa = productJpa;
    }

    @Override
    public boolean hasProducts(Long categoryId) {
        return productJpa.existsAnyByCategoryId(categoryId);
    }
}
