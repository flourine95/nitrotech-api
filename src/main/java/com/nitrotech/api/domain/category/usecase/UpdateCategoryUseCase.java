package com.nitrotech.api.domain.category.usecase;

import com.nitrotech.api.domain.category.dto.CategoryData;
import com.nitrotech.api.domain.category.dto.UpdateCategoryCommand;
import com.nitrotech.api.domain.category.exception.CategoryCircularReferenceException;
import com.nitrotech.api.domain.category.exception.CategoryNotFoundException;
import com.nitrotech.api.domain.category.exception.CategorySlugExistsException;
import com.nitrotech.api.domain.category.repository.CategoryRepository;
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
            throw CategoryNotFoundException.withId(command.id());
        }
        if (command.slug() != null && categoryRepository.existsNotDeletedBySlugAndIdNot(command.slug(), command.id())) {
            throw new CategorySlugExistsException(command.slug());
        }
        if (command.parentId() != null) {
            if (!categoryRepository.existsById(command.parentId())) {
                throw CategoryNotFoundException.parentWithId(command.parentId());
            }
            if (command.parentId().equals(command.id())) {
                throw new CategoryCircularReferenceException("Category cannot be its own parent");
            }
            if (categoryRepository.isDescendantOf(command.parentId(), command.id())) {
                throw new CategoryCircularReferenceException("Cannot set parent: circular reference detected");
            }
        }
        return categoryRepository.update(command);
    }
}
