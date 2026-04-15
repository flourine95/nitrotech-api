package com.nitrotech.api.infrastructure.persistence.repository;

import com.nitrotech.api.domain.brand.usecase.ProductBrandChecker;
import org.springframework.stereotype.Component;

@Component
public class ProductBrandCheckerImpl implements ProductBrandChecker {

    private final ProductJpaRepository productJpa;

    public ProductBrandCheckerImpl(ProductJpaRepository productJpa) {
        this.productJpa = productJpa;
    }

    @Override
    public boolean hasProducts(Long brandId) {
        return productJpa.existsAnyByBrandId(brandId);
    }
}
