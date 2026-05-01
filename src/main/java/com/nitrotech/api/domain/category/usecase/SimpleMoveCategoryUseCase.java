package com.nitrotech.api.domain.category.usecase;

import com.nitrotech.api.domain.category.dto.CategoryData;
import com.nitrotech.api.domain.category.repository.CategoryRepository;
import org.springframework.stereotype.Service;

@Service
public class SimpleMoveCategoryUseCase {

    private final CategoryRepository categoryRepository;

    public SimpleMoveCategoryUseCase(CategoryRepository categoryRepository) {
        this.categoryRepository = categoryRepository;
    }

    public CategoryData execute(Long id, Long newParentId, Long afterId) {
        return categoryRepository.move(id, newParentId, afterId);
    }
}
