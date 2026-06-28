package com.nitrotech.api.domain.category.usecase;

import com.nitrotech.api.domain.category.dto.CategoryData;
import com.nitrotech.api.domain.category.exception.CategoryNotFoundException;
import com.nitrotech.api.domain.category.repository.CategoryRepository;
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
                    .orElseThrow(() -> CategoryNotFoundException.withId(id));
        } catch (NumberFormatException e) {
            return categoryRepository.findVisibleBySlug(idOrSlug)
                    .orElseThrow(() -> CategoryNotFoundException.withSlug(idOrSlug));
        }
    }
}
