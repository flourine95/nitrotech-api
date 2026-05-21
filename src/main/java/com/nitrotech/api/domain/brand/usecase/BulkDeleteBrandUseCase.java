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
public class BulkDeleteBrandUseCase {

    private final BrandRepository brandRepository;

    public BulkResult execute(List<Long> ids) {
        List<Long> deleted = brandRepository.bulkSoftDelete(ids);
        Set<Long> deletedSet = Set.copyOf(deleted);

        Map<Long, String> failedReasons = new LinkedHashMap<>();
        for (Long id : ids) {
            if (!deletedSet.contains(id)) {
                failedReasons.put(id, "Brand not found or already deleted");
            }
        }

        List<Long> failed = List.copyOf(failedReasons.keySet());
        return new BulkResult(deleted.size(), failed.size(), failed, failedReasons);
    }
}
