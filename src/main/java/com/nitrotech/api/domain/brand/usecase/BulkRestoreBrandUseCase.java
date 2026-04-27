package com.nitrotech.api.domain.brand.usecase;

import com.nitrotech.api.domain.brand.dto.BulkResult;
import com.nitrotech.api.domain.brand.repository.BrandRepository;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Set;

@Service
public class BulkRestoreBrandUseCase {

    private final BrandRepository brandRepository;

    public BulkRestoreBrandUseCase(BrandRepository brandRepository) {
        this.brandRepository = brandRepository;
    }

    public BulkResult execute(List<Long> ids) {
        List<Long> restored = brandRepository.bulkRestore(ids);
        Set<Long> restoredSet = Set.copyOf(restored);
        List<Long> failed = ids.stream().filter(id -> !restoredSet.contains(id)).toList();
        return new BulkResult(restored.size(), failed.size(), failed);
    }
}
