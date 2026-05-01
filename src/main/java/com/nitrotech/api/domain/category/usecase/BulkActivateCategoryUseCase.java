package com.nitrotech.api.domain.category.usecase;

import com.nitrotech.api.domain.category.dto.BulkResult;
import com.nitrotech.api.domain.category.repository.CategoryRepository;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;
import java.util.Set;

@Service
public class BulkActivateCategoryUseCase {

    private final CategoryRepository categoryRepository;

    public BulkActivateCategoryUseCase(CategoryRepository categoryRepository) {
        this.categoryRepository = categoryRepository;
    }

    public BulkResult execute(List<Long> ids) {
        List<Long> activated = categoryRepository.bulkActivate(ids);
        Set<Long> activatedSet = Set.copyOf(activated);

        Map<Long, String> failedReasons = new java.util.LinkedHashMap<>();
        for (Long id : ids) {
            if (!activatedSet.contains(id)) {
                failedReasons.put(id, "Category not found or already deleted");
            }
        }

        List<Long> failed = List.copyOf(failedReasons.keySet());
        return new BulkResult(activated.size(), failed.size(), failed, failedReasons);
    }
}
