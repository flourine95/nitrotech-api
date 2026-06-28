package com.nitrotech.api.domain.category.usecase;

import com.nitrotech.api.domain.category.exception.CategoryNotFoundException;
import com.nitrotech.api.domain.category.exception.CategorySlugConflictException;

import com.nitrotech.api.domain.category.repository.CategoryRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class RestoreCategoryUseCase {

    private final CategoryRepository categoryRepository;

    public void execute(Long id) {
        // Tìm record đã deleted — nếu không tìm thấy thì 404
        var category = categoryRepository.findDeletedById(id)
                .orElseThrow(() -> new CategoryNotFoundException("Deleted category not found"));

        // Check slug conflict với records chưa bị xóa
        if (categoryRepository.existsNotDeletedBySlugAndIdNot(category.slug(), id)) {
            throw new CategorySlugConflictException(category.slug());
        }

        categoryRepository.restore(id);
    }
}
