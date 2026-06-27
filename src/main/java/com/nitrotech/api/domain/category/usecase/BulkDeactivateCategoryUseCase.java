package com.nitrotech.api.domain.category.usecase;

import com.nitrotech.api.domain.category.dto.BulkResult;
import com.nitrotech.api.domain.category.repository.CategoryRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

@Service
@RequiredArgsConstructor
public class BulkDeactivateCategoryUseCase {

    private final CategoryRepository categoryRepository;

    public BulkResult execute(List<Long> ids) {
        List<Long> deactivated = categoryRepository.bulkDeactivate(ids);
        Set<Long> deactivatedSet = Set.copyOf(deactivated);

        Map<Long, String> failedReasons = new LinkedHashMap<>();
        for (Long id : ids) {
            if (!deactivatedSet.contains(id)) {
                failedReasons.put(id, "Category not found or already deleted");
            }
        }

        List<Long> failed = List.copyOf(failedReasons.keySet());
        return new BulkResult(deactivated.size(), failed.size(), failed, failedReasons);
    }
}
