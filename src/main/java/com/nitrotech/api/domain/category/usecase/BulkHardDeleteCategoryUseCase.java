package com.nitrotech.api.domain.category.usecase;

import com.nitrotech.api.domain.category.dto.BulkResult;
import com.nitrotech.api.domain.category.repository.CategoryRepository;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;
import java.util.Set;

@Service
public class BulkHardDeleteCategoryUseCase {

    private final CategoryRepository categoryRepository;

    public BulkHardDeleteCategoryUseCase(CategoryRepository categoryRepository) {
        this.categoryRepository = categoryRepository;
    }

    public BulkResult execute(List<Long> ids) {
        List<Long> deleted = categoryRepository.bulkHardDelete(ids);
        Set<Long> deletedSet = Set.copyOf(deleted);

        Map<Long, String> failedReasons = new java.util.LinkedHashMap<>();
        for (Long id : ids) {
            if (!deletedSet.contains(id)) {
                failedReasons.put(id, "Category not found, not soft-deleted yet, or has children");
            }
        }

        List<Long> failed = List.copyOf(failedReasons.keySet());
        return new BulkResult(deleted.size(), failed.size(), failed, failedReasons);
    }
}
