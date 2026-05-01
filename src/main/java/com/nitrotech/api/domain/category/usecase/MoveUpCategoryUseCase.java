package com.nitrotech.api.domain.category.usecase;

import com.nitrotech.api.domain.category.dto.CategoryData;
import com.nitrotech.api.domain.category.repository.CategoryRepository;
import org.springframework.stereotype.Service;

@Service
public class MoveUpCategoryUseCase {

    private final CategoryRepository categoryRepository;

    public MoveUpCategoryUseCase(CategoryRepository categoryRepository) {
        this.categoryRepository = categoryRepository;
    }

    public CategoryData execute(Long id) {
        return categoryRepository.moveUp(id);
    }
}
