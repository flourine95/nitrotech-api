package com.nitrotech.api.domain.category.usecase;

import com.nitrotech.api.domain.category.repository.CategoryRepository;
import com.nitrotech.api.shared.exception.ConflictException;
import com.nitrotech.api.shared.exception.NotFoundException;
import org.springframework.stereotype.Service;

@Service
public class HardDeleteCategoryUseCase {

    private final CategoryRepository categoryRepository;
    private final ProductCategoryChecker productCategoryChecker;

    public HardDeleteCategoryUseCase(CategoryRepository categoryRepository,
                                     ProductCategoryChecker productCategoryChecker) {
        this.categoryRepository = categoryRepository;
        this.productCategoryChecker = productCategoryChecker;
    }

    public void execute(Long id) {
        // Chỉ cho hard delete record đã soft deleted
        categoryRepository.findDeletedById(id)
                .orElseThrow(() -> new NotFoundException("CATEGORY_NOT_FOUND",
                        "Deleted category not found. Soft delete first before permanent delete."));

        // Block nếu còn children (kể cả deleted)
        if (categoryRepository.hasAnyChildren(id)) {
            throw new ConflictException("CATEGORY_HAS_CHILDREN",
                    "Cannot permanently delete category with subcategories.");
        }

        // Block nếu có product đang dùng category này
        if (productCategoryChecker.hasProducts(id)) {
            throw new ConflictException("CATEGORY_HAS_PRODUCTS",
                    "Cannot permanently delete category that has products.");
        }

        categoryRepository.hardDelete(id);
    }
}
