package com.nitrotech.api.domain.category.usecase;

import com.nitrotech.api.domain.category.dto.CategoryData;
import com.nitrotech.api.domain.category.exception.CategoryNotFoundException;
import com.nitrotech.api.domain.category.repository.CategoryRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class GetCategoryUseCase {

    private final CategoryRepository categoryRepository;

    public CategoryData execute(String idOrSlug) {
        try {
            Long id = Long.parseLong(idOrSlug);
            return categoryRepository.findNotDeletedById(id)
                    .orElseThrow(() -> CategoryNotFoundException.withId(id));
        } catch (NumberFormatException e) {
            return categoryRepository.findNotDeletedBySlug(idOrSlug)
                    .orElseThrow(() -> CategoryNotFoundException.withSlug(idOrSlug));
        }
    }
}
