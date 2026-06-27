package com.nitrotech.api.domain.category.usecase;

import com.nitrotech.api.domain.category.dto.CategoryData;
import com.nitrotech.api.domain.category.dto.CreateCategoryCommand;
import com.nitrotech.api.domain.category.exception.CategoryNotFoundException;
import com.nitrotech.api.domain.category.exception.CategorySlugExistsException;
import com.nitrotech.api.domain.category.repository.CategoryRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class CreateCategoryUseCase {

    private final CategoryRepository categoryRepository;

    public CategoryData execute(CreateCategoryCommand command) {
        if (categoryRepository.existsNotDeletedBySlug(command.slug())) {
            throw new CategorySlugExistsException(command.slug());
        }
        if (command.parentId() != null && !categoryRepository.existsById(command.parentId())) {
            throw CategoryNotFoundException.parentWithId(command.parentId());
        }
        return categoryRepository.create(command);
    }
}
