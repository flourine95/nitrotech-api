package com.nitrotech.api.domain.category.usecase;

import com.nitrotech.api.domain.category.repository.CategoryRepository;
import com.nitrotech.api.shared.exception.ConflictException;
import com.nitrotech.api.shared.exception.NotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class RestoreCategoryUseCase {

    private final CategoryRepository categoryRepository;

    public void execute(Long id) {
        // Tìm record đã deleted — nếu không tìm thấy thì 404
        var category = categoryRepository.findDeletedById(id)
                .orElseThrow(() -> new NotFoundException("CATEGORY_NOT_FOUND",
                        "Deleted category not found"));

        // Check slug conflict với records chưa bị xóa
        if (categoryRepository.existsNotDeletedBySlugAndIdNot(category.slug(), id)) {
            throw new ConflictException("CATEGORY_SLUG_CONFLICT",
                    "Cannot restore: slug '" + category.slug() + "' is already used by another category");
        }

        categoryRepository.restore(id);
    }
}
