package com.nitrotech.api.domain.category.usecase;

import com.nitrotech.api.domain.category.repository.CategoryRepository;
import com.nitrotech.api.shared.exception.ConflictException;
import com.nitrotech.api.shared.exception.NotFoundException;
import org.springframework.stereotype.Service;

@Service
public class RestoreCategoryUseCase {

    private final CategoryRepository categoryRepository;

    public RestoreCategoryUseCase(CategoryRepository categoryRepository) {
        this.categoryRepository = categoryRepository;
    }

    public void execute(Long id) {
        // Tìm record đã deleted — nếu không tìm thấy thì 404
        var category = categoryRepository.findDeletedById(id)
                .orElseThrow(() -> new NotFoundException("CATEGORY_NOT_FOUND",
                        "Deleted category not found"));

        // Check slug conflict với active records
        if (categoryRepository.existsActiveBySlugAndIdNot(category.slug(), id)) {
            throw new ConflictException("CATEGORY_SLUG_CONFLICT",
                    "Cannot restore: slug '" + category.slug() + "' is already used by another active category");
        }

        categoryRepository.restore(id);
    }
}
