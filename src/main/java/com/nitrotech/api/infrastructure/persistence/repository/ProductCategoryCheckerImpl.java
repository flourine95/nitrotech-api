package com.nitrotech.api.infrastructure.persistence.repository;

import com.nitrotech.api.domain.category.usecase.ProductCategoryChecker;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Set;

@Component
@RequiredArgsConstructor
public class ProductCategoryCheckerImpl implements ProductCategoryChecker {

    private final ProductJpaRepository productJpa;

    @Override
    public boolean hasProducts(Long categoryId) {
        return productJpa.existsAnyByCategoryId(categoryId);
    }

    @Override
    public Set<Long> filterHasProducts(List<Long> categoryIds) {
        if (categoryIds == null || categoryIds.isEmpty()) return Set.of();
        return Set.copyOf(productJpa.findCategoryIdsWithProducts(categoryIds));
    }
}
