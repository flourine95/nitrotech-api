package com.nitrotech.api.domain.category.usecase;

import com.nitrotech.api.domain.category.dto.BulkResult;
import com.nitrotech.api.domain.category.repository.CategoryRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.*;

@Service
@RequiredArgsConstructor
public class BulkRestoreCategoryUseCase {

    private final CategoryRepository categoryRepository;

    public BulkResult execute(List<Long> ids) {
        List<Long> eligible = new ArrayList<>();
        Map<Long, String> failedReasons = new LinkedHashMap<>();

        for (Long id : ids) {
            var category = categoryRepository.findDeletedById(id);
            if (category.isEmpty()) {
                failedReasons.put(id, "Category not found or not deleted");
                continue;
            }
            if (categoryRepository.existsNotDeletedBySlugAndIdNot(category.get().slug(), id)) {
                failedReasons.put(id, "Category slug is already used by another category");
                continue;
            }
            eligible.add(id);
        }

        List<Long> restored = categoryRepository.bulkRestore(eligible);
        Set<Long> restoredSet = Set.copyOf(restored);

        for (Long id : ids) {
            if (!restoredSet.contains(id) && !failedReasons.containsKey(id)) {
                failedReasons.put(id, "Category not found or not deleted");
            }
        }

        List<Long> failed = List.copyOf(failedReasons.keySet());
        return new BulkResult(restored.size(), failed.size(), failed, failedReasons);
    }
}
