package com.nitrotech.api.domain.category.usecase;

import com.nitrotech.api.domain.category.exception.CategoryCircularReferenceException;
import com.nitrotech.api.domain.category.exception.CategoryNotFoundException;

import com.nitrotech.api.domain.category.dto.MoveCategoryCommand;
import com.nitrotech.api.domain.category.dto.MoveCategoryResult;
import com.nitrotech.api.domain.category.repository.CategoryRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class MoveCategoryUseCase {

    private final CategoryRepository categoryRepository;

    public MoveCategoryResult execute(MoveCategoryCommand command) {
        if (!categoryRepository.existsById(command.movedId())) {
            throw new CategoryNotFoundException("Category with ID " + command.movedId() + " not found");
        }

        if (command.toParentId() != null) {
            if (!categoryRepository.existsById(command.toParentId())) {
                throw new CategoryNotFoundException("Target parent category with ID " + command.toParentId() + " not found");
            }
            if (command.toParentId().equals(command.movedId())) {
                throw new CategoryCircularReferenceException("Category cannot be its own parent");
            }
            if (categoryRepository.isDescendantOf(command.toParentId(), command.movedId())) {
                throw new CategoryCircularReferenceException("Cannot move category into its own descendant");
            }
        }

        return categoryRepository.moveCategory(command);
    }
}
