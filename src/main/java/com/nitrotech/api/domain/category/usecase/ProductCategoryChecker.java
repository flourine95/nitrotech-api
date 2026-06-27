package com.nitrotech.api.domain.category.usecase;

import java.util.List;
import java.util.Set;

public interface ProductCategoryChecker {
    boolean hasProducts(Long categoryId);

    Set<Long> filterHasProducts(List<Long> categoryIds);
}
