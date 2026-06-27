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
public class BulkDeleteCategoryUseCase {

    private final CategoryRepository categoryRepository;

    public BulkResult execute(List<Long> ids) {
        List<Long> deleted = categoryRepository.bulkSoftDelete(ids);
        Set<Long> deletedSet = Set.copyOf(deleted);

        Map<Long, String> failedReasons = new LinkedHashMap<>();
        for (Long id : ids) {
            if (!deletedSet.contains(id)) {
                failedReasons.put(id, "Category not found, already deleted, or has children");
            }
        }

        List<Long> failed = List.copyOf(failedReasons.keySet());
        return new BulkResult(deleted.size(), failed.size(), failed, failedReasons);
    }
}
