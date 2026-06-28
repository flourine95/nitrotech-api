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
public class BulkHardDeleteCategoryUseCase {

    private final CategoryRepository categoryRepository;
    private final ProductCategoryChecker productCategoryChecker;

    public BulkResult execute(List<Long> ids) {
        Set<Long> hasProducts = productCategoryChecker.filterHasProducts(ids);
        List<Long> eligible = ids.stream()
                .filter(id -> !hasProducts.contains(id))
                .toList();

        List<Long> deleted = categoryRepository.bulkHardDelete(eligible);
        Set<Long> deletedSet = Set.copyOf(deleted);

        Map<Long, String> failedReasons = new LinkedHashMap<>();
        for (Long id : ids) {
            if (deletedSet.contains(id)) {
                continue;
            }
            if (hasProducts.contains(id)) {
                failedReasons.put(id, "Category still has active products");
            } else {
                failedReasons.put(id, "Category not found, not soft-deleted yet, or has children");
            }
        }

        List<Long> failed = List.copyOf(failedReasons.keySet());
        return new BulkResult(deleted.size(), failed.size(), failed, failedReasons);
    }
}
