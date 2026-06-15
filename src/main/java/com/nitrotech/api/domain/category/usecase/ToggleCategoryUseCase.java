package com.nitrotech.api.domain.category.usecase;

import com.nitrotech.api.domain.category.dto.CategoryData;
import com.nitrotech.api.domain.category.dto.UpdateCategoryCommand;
import com.nitrotech.api.domain.category.repository.CategoryRepository;
import com.nitrotech.api.shared.exception.NotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class ToggleCategoryUseCase {

    private final CategoryRepository categoryRepository;

    @Transactional
    public CategoryData execute(Long id) {
        CategoryData category = categoryRepository.findNotDeletedById(id)
                .orElseThrow(() -> new NotFoundException("CATEGORY_NOT_FOUND", "Category not found"));
        
        // Toggle active status
        boolean newActiveStatus = !category.active();
        
        return categoryRepository.update(new UpdateCategoryCommand(
                id,
                null,  // name
                null,  // slug
                null,  // description
                null,  // image
                null,  // parentId
                newActiveStatus  // active
        ));
    }
}
