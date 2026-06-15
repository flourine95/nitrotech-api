package com.nitrotech.api.domain.brand.usecase;

import com.nitrotech.api.domain.brand.dto.BulkResult;
import com.nitrotech.api.domain.brand.repository.BrandRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;

@Service
@RequiredArgsConstructor
public class BulkRestoreBrandUseCase {

    private final BrandRepository brandRepository;

    public BulkResult execute(List<Long> ids) {
        List<Long> eligible = new ArrayList<>();
        Map<Long, String> failedReasons = new LinkedHashMap<>();

        for (Long id : ids) {
            var brand = brandRepository.findDeletedById(id);
            if (brand.isEmpty()) {
                failedReasons.put(id, "Brand not found or not deleted");
                continue;
            }
            if (brandRepository.existsNotDeletedBySlugAndIdNot(brand.get().slug(), id)) {
                failedReasons.put(id, "Brand slug is already used by another active brand");
                continue;
            }
            eligible.add(id);
        }

        List<Long> restored = brandRepository.bulkRestore(eligible);
        Set<Long> restoredSet = Set.copyOf(restored);

        for (Long id : ids) {
            if (!restoredSet.contains(id) && !failedReasons.containsKey(id)) {
                failedReasons.put(id, "Brand not found or not deleted");
            }
        }

        List<Long> failed = List.copyOf(failedReasons.keySet());
        return new BulkResult(restored.size(), failed.size(), failed, failedReasons);
    }
}
