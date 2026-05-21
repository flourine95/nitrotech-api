package com.nitrotech.api.domain.category.usecase;

import com.nitrotech.api.domain.category.dto.CategoryData;
import com.nitrotech.api.domain.category.repository.CategoryRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class SimpleMoveCategoryUseCase {

    private final CategoryRepository categoryRepository;

    public CategoryData execute(Long id, Long newParentId, Long afterId) {
        return categoryRepository.move(id, newParentId, afterId);
    }
}
