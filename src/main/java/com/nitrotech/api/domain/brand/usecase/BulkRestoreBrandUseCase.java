package com.nitrotech.api.domain.brand.usecase;

import com.nitrotech.api.domain.brand.dto.BulkResult;
import com.nitrotech.api.domain.brand.repository.BrandRepository;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;
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

        Map<Long, String> failedReasons = new java.util.LinkedHashMap<>();
        for (Long id : ids) {
            if (!restoredSet.contains(id)) {
                failedReasons.put(id, "Brand not found or not deleted");
            }
        }

        List<Long> failed = List.copyOf(failedReasons.keySet());
        return new BulkResult(restored.size(), failed.size(), failed, failedReasons);
    }
}
