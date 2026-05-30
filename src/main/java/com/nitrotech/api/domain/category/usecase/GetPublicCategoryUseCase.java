package com.nitrotech.api.domain.category.usecase;

import com.nitrotech.api.domain.category.dto.CategoryData;
import com.nitrotech.api.domain.category.repository.CategoryRepository;
import com.nitrotech.api.shared.exception.NotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class GetPublicCategoryUseCase {

    private final CategoryRepository categoryRepository;

    public CategoryData execute(String idOrSlug) {
        try {
            Long id = Long.parseLong(idOrSlug);
            return categoryRepository.findVisibleById(id)
                    .orElseThrow(() -> new NotFoundException("CATEGORY_NOT_FOUND",
                            "Category with ID " + id + " not found"));
        } catch (NumberFormatException e) {
            return categoryRepository.findVisibleBySlug(idOrSlug)
                    .orElseThrow(() -> new NotFoundException("CATEGORY_NOT_FOUND",
                            "Category with slug '" + idOrSlug + "' not found"));
        }
    }
}
