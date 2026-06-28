package com.nitrotech.api.domain.category.usecase;

import com.nitrotech.api.domain.category.exception.CategoryHasChildrenException;
import com.nitrotech.api.domain.category.exception.CategoryNotFoundException;
import com.nitrotech.api.domain.category.repository.CategoryRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class DeleteCategoryUseCase {

    private final CategoryRepository categoryRepository;

    public void execute(Long id) {
        if (!categoryRepository.existsById(id)) {
            throw CategoryNotFoundException.withId(id);
        }
        if (categoryRepository.hasNotDeletedChildren(id)) {
            throw new CategoryHasChildrenException("Cannot delete category with subcategories. Delete or move them first.");
        }
        categoryRepository.softDelete(id);
    }
}
