package com.nitrotech.api.infrastructure.persistence.repository;

import com.nitrotech.api.domain.brand.usecase.ProductBrandChecker;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Set;

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

    @Override
    public Set<Long> filterHasProducts(List<Long> brandIds) {
        if (brandIds == null || brandIds.isEmpty()) return Set.of();
        return Set.copyOf(productJpa.findBrandIdsWithProducts(brandIds));
    }
}
