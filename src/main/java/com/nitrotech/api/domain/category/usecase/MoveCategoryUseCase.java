package com.nitrotech.api.domain.category.usecase;

import com.nitrotech.api.domain.category.dto.MoveCategoryCommand;
import com.nitrotech.api.domain.category.dto.MoveCategoryResult;
import com.nitrotech.api.domain.category.repository.CategoryRepository;
import com.nitrotech.api.shared.exception.ConflictException;
import com.nitrotech.api.shared.exception.NotFoundException;
import org.springframework.stereotype.Service;

@Service
public class MoveCategoryUseCase {

    private final CategoryRepository categoryRepository;

    public MoveCategoryUseCase(CategoryRepository categoryRepository) {
        this.categoryRepository = categoryRepository;
    }

    public MoveCategoryResult execute(MoveCategoryCommand command) {
        if (!categoryRepository.existsById(command.movedId())) {
            throw new NotFoundException("CATEGORY_NOT_FOUND", "Category not found");
        }

        if (command.toParentId() != null) {
            if (!categoryRepository.existsById(command.toParentId())) {
                throw new NotFoundException("CATEGORY_NOT_FOUND", "Target parent category not found");
            }
            if (command.toParentId().equals(command.movedId())) {
                throw new ConflictException("CATEGORY_CIRCULAR_REF", "Category cannot be its own parent");
            }
            if (categoryRepository.isDescendantOf(command.toParentId(), command.movedId())) {
                throw new ConflictException("CATEGORY_CIRCULAR_REF", "Cannot move category into its own descendant");
            }
        }

        return categoryRepository.moveCategory(command);
    }
}
