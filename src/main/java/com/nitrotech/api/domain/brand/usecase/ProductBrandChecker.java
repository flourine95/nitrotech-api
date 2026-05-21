package com.nitrotech.api.domain.brand.usecase;

import java.util.List;
import java.util.Set;

public interface ProductBrandChecker {
    boolean hasProducts(Long brandId);
    Set<Long> filterHasProducts(List<Long> brandIds); // batch check, tránh N+1
}
