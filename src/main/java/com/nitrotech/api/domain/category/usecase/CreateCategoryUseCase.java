package com.nitrotech.api.domain.category.usecase;

import com.nitrotech.api.domain.category.dto.CategoryData;
import com.nitrotech.api.domain.category.dto.CreateCategoryCommand;
import com.nitrotech.api.domain.category.repository.CategoryRepository;
import com.nitrotech.api.shared.exception.ConflictException;
import com.nitrotech.api.shared.exception.NotFoundException;
import org.springframework.stereotype.Service;

@Service
public class CreateCategoryUseCase {

    private final CategoryRepository categoryRepository;

    public CreateCategoryUseCase(CategoryRepository categoryRepository) {
        this.categoryRepository = categoryRepository;
    }

    public CategoryData execute(CreateCategoryCommand command) {
        if (categoryRepository.existsBySlug(command.slug())) {
            throw new ConflictException("CATEGORY_SLUG_EXISTS", "Slug already exists");
        }
        if (command.parentId() != null && !categoryRepository.existsById(command.parentId())) {
            throw new NotFoundException("CATEGORY_NOT_FOUND", "Parent category not found");
        }
        return categoryRepository.create(command);
    }
}
