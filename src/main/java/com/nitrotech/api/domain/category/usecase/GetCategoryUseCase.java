package com.nitrotech.api.domain.category.usecase;

import com.nitrotech.api.domain.category.dto.CategoryData;
import com.nitrotech.api.domain.category.repository.CategoryRepository;
import com.nitrotech.api.shared.exception.NotFoundException;
import org.springframework.stereotype.Service;

@Service
public class GetCategoryUseCase {

    private final CategoryRepository categoryRepository;

    public GetCategoryUseCase(CategoryRepository categoryRepository) {
        this.categoryRepository = categoryRepository;
    }

    /**
     * Get category by ID or slug (auto-detect)
     * @param idOrSlug numeric ID or string slug
     */
    public CategoryData execute(String idOrSlug) {
        // Try to parse as Long (ID)
        try {
            Long id = Long.parseLong(idOrSlug);
            return categoryRepository.findById(id)
                    .orElseThrow(() -> new NotFoundException("CATEGORY_NOT_FOUND", 
                            "Category with ID " + id + " not found"));
        } catch (NumberFormatException e) {
            // Not a number, treat as slug
            return categoryRepository.findBySlug(idOrSlug)
                    .orElseThrow(() -> new NotFoundException("CATEGORY_NOT_FOUND", 
                            "Category with slug '" + idOrSlug + "' not found"));
        }
    }

    /**
     * Get category by ID (deprecated, use execute(String) instead)
     */
    @Deprecated
    public CategoryData execute(Long id) {
        return categoryRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("CATEGORY_NOT_FOUND", 
                        "Category with ID " + id + " not found"));
    }
}
