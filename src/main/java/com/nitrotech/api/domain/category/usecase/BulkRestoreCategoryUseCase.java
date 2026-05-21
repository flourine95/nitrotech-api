package com.nitrotech.api.domain.category.usecase;

import com.nitrotech.api.domain.category.dto.BulkResult;
import com.nitrotech.api.domain.category.repository.CategoryRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;
import java.util.Set;

@Service
@RequiredArgsConstructor
public class BulkRestoreCategoryUseCase {

    private final CategoryRepository categoryRepository;

    public BulkResult execute(List<Long> ids) {
        List<Long> restored = categoryRepository.bulkRestore(ids);
        Set<Long> restoredSet = Set.copyOf(restored);

        Map<Long, String> failedReasons = new java.util.LinkedHashMap<>();
        for (Long id : ids) {
            if (!restoredSet.contains(id)) {
                failedReasons.put(id, "Category not found or not deleted");
            }
        }

        List<Long> failed = List.copyOf(failedReasons.keySet());
        return new BulkResult(restored.size(), failed.size(), failed, failedReasons);
    }
}
