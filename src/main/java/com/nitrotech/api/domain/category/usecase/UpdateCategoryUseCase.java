package com.nitrotech.api.domain.category.usecase;

import com.nitrotech.api.domain.category.dto.CategoryData;
import com.nitrotech.api.domain.category.dto.UpdateCategoryCommand;
import com.nitrotech.api.domain.category.repository.CategoryRepository;
import com.nitrotech.api.shared.exception.ConflictException;
import com.nitrotech.api.shared.exception.NotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class UpdateCategoryUseCase {

    private final CategoryRepository categoryRepository;

    @Transactional
    public CategoryData execute(UpdateCategoryCommand command) {
        if (!categoryRepository.existsById(command.id())) {
            throw new NotFoundException("CATEGORY_NOT_FOUND", 
                    "Category with ID " + command.id() + " not found");
        }
        if (command.slug() != null && categoryRepository.existsNotDeletedBySlugAndIdNot(command.slug(), command.id())) {
            throw new ConflictException("CATEGORY_SLUG_EXISTS", 
                    "Slug '" + command.slug() + "' already exists");
        }
        if (command.parentId() != null) {
            if (!categoryRepository.existsById(command.parentId())) {
                throw new NotFoundException("CATEGORY_NOT_FOUND", 
                        "Parent category with ID " + command.parentId() + " not found");
            }
            if (command.parentId().equals(command.id())) {
                throw new ConflictException("CATEGORY_CIRCULAR_REF", 
                        "Category cannot be its own parent");
            }
            if (categoryRepository.isDescendantOf(command.parentId(), command.id())) {
                throw new ConflictException("CATEGORY_CIRCULAR_REF", 
                        "Cannot set parent: circular reference detected");
            }
        }
        return categoryRepository.update(command);
    }
}
