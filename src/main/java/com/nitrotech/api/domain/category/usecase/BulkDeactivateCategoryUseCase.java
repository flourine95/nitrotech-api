package com.nitrotech.api.domain.category.usecase;

import com.nitrotech.api.domain.category.dto.BulkResult;
import com.nitrotech.api.domain.category.repository.CategoryRepository;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;
import java.util.Set;

@Service
public class BulkDeactivateCategoryUseCase {

    private final CategoryRepository categoryRepository;

    public BulkDeactivateCategoryUseCase(CategoryRepository categoryRepository) {
        this.categoryRepository = categoryRepository;
    }

    public BulkResult execute(List<Long> ids) {
        List<Long> deactivated = categoryRepository.bulkDeactivate(ids);
        Set<Long> deactivatedSet = Set.copyOf(deactivated);

        Map<Long, String> failedReasons = new java.util.LinkedHashMap<>();
        for (Long id : ids) {
            if (!deactivatedSet.contains(id)) {
                failedReasons.put(id, "Category not found or already deleted");
            }
        }

        List<Long> failed = List.copyOf(failedReasons.keySet());
        return new BulkResult(deactivated.size(), failed.size(), failed, failedReasons);
    }
}
