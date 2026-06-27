package com.nitrotech.api.domain.category.usecase;

import com.nitrotech.api.domain.category.exception.CategoryHasChildrenException;
import com.nitrotech.api.domain.category.exception.CategoryHasProductsException;
import com.nitrotech.api.domain.category.exception.CategoryNotFoundException;

import com.nitrotech.api.domain.category.repository.CategoryRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class HardDeleteCategoryUseCase {

    private final CategoryRepository categoryRepository;
    private final ProductCategoryChecker productCategoryChecker;

    public void execute(Long id) {
        // Chỉ cho hard delete record đã soft deleted
        categoryRepository.findDeletedById(id)
                .orElseThrow(CategoryNotFoundException::deletedForHardDelete);

        // Block nếu còn children (kể cả deleted)
        if (categoryRepository.hasAnyChildren(id)) {
            throw new CategoryHasChildrenException("Cannot permanently delete category with subcategories.");
        }

        // Block nếu có product đang dùng category này
        if (productCategoryChecker.hasProducts(id)) {
            throw new CategoryHasProductsException();
        }

        categoryRepository.hardDelete(id);
    }
}
