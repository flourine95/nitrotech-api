package com.nitrotech.api.domain.category.usecase;

import com.nitrotech.api.domain.category.dto.ValidateDeleteResult;
import com.nitrotech.api.domain.category.repository.CategoryRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class ValidateBulkDeleteCategoryUseCase {

    private final CategoryRepository categoryRepository;

    public ValidateDeleteResult execute(List<Long> ids) {
        List<Long> canDelete = new ArrayList<>();
        List<Long> cannotDelete = new ArrayList<>();
        Map<Long, String> reasons = new LinkedHashMap<>();

        for (Long id : ids) {
            if (!categoryRepository.existsById(id)) {
                cannotDelete.add(id);
                reasons.put(id, "Category not found or already deleted");
            } else if (categoryRepository.hasAnyChildren(id)) {
                cannotDelete.add(id);
                int childCount = categoryRepository.hasNotDeletedChildren(id) ? 
                    countChildren(id) : countAllChildren(id);
                reasons.put(id, "Has " + childCount + " children");
            } else {
                canDelete.add(id);
            }
        }

        return new ValidateDeleteResult(canDelete, cannotDelete, reasons);
    }

    private int countChildren(Long parentId) {
        // Simplified: just return 1+ for now
        // In production, you might want to add a proper count query
        return 1;
    }

    private int countAllChildren(Long parentId) {
        return 1;
    }
}
