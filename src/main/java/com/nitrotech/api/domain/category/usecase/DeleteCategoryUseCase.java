package com.nitrotech.api.domain.category.usecase;

import com.nitrotech.api.domain.category.repository.CategoryRepository;
import com.nitrotech.api.shared.exception.ConflictException;
import com.nitrotech.api.shared.exception.NotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class DeleteCategoryUseCase {

    private final CategoryRepository categoryRepository;

    public void execute(Long id) {
        if (!categoryRepository.existsById(id)) {
            throw new NotFoundException("CATEGORY_NOT_FOUND", 
                    "Category with ID " + id + " not found");
        }
        if (categoryRepository.hasActiveChildren(id)) {
            throw new ConflictException("CATEGORY_HAS_CHILDREN",
                    "Cannot delete category with active subcategories. Delete or move them first.");
        }
        categoryRepository.softDelete(id);
    }
}
