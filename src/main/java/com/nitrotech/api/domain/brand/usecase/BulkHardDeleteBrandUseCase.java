package com.nitrotech.api.domain.brand.usecase;

import com.nitrotech.api.domain.brand.dto.BulkResult;
import com.nitrotech.api.domain.brand.repository.BrandRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;

@Service
@RequiredArgsConstructor
public class BulkHardDeleteBrandUseCase {

    private final BrandRepository brandRepository;
    private final ProductBrandChecker productBrandChecker;

    public BulkResult execute(List<Long> ids) {
        Set<Long> hasProducts = productBrandChecker.filterHasProducts(ids);
        List<Long> eligible = ids.stream()
                .filter(id -> !hasProducts.contains(id))
                .toList();

        List<Long> deleted = brandRepository.bulkHardDelete(eligible);
        Set<Long> deletedSet = Set.copyOf(deleted);

        Map<Long, String> failedReasons = new LinkedHashMap<>();
        for (Long id : ids) {
            if (deletedSet.contains(id)) {
                continue;
            }
            if (hasProducts.contains(id)) {
                failedReasons.put(id, "Brand still has active products");
            } else {
                failedReasons.put(id, "Brand not found or not soft-deleted yet");
            }
        }

        List<Long> failed = List.copyOf(failedReasons.keySet());
        return new BulkResult(deleted.size(), failed.size(), failed, failedReasons);
    }
}
