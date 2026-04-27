package com.nitrotech.api.domain.brand.usecase;

import com.nitrotech.api.domain.brand.dto.BulkResult;
import com.nitrotech.api.domain.brand.repository.BrandRepository;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Set;

@Service
public class BulkHardDeleteBrandUseCase {

    private final BrandRepository brandRepository;
    private final ProductBrandChecker productBrandChecker;

    public BulkHardDeleteBrandUseCase(BrandRepository brandRepository,
                                       ProductBrandChecker productBrandChecker) {
        this.brandRepository = brandRepository;
        this.productBrandChecker = productBrandChecker;
    }

    public BulkResult execute(List<Long> ids) {
        // 1 query batch thay vì N queries
        Set<Long> hasProducts = productBrandChecker.filterHasProducts(ids);
        List<Long> eligible = ids.stream().filter(id -> !hasProducts.contains(id)).toList();

        List<Long> deleted = brandRepository.bulkHardDelete(eligible);
        Set<Long> deletedSet = Set.copyOf(deleted);
        List<Long> failed = ids.stream().filter(id -> !deletedSet.contains(id)).toList();
        return new BulkResult(deleted.size(), failed.size(), failed);
    }
}
