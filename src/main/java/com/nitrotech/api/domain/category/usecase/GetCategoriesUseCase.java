package com.nitrotech.api.domain.category.usecase;

import com.nitrotech.api.domain.category.dto.CategoryData;
import com.nitrotech.api.domain.category.repository.CategoryRepository;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class GetCategoriesUseCase {

    private final CategoryRepository categoryRepository;

    public GetCategoriesUseCase(CategoryRepository categoryRepository) {
        this.categoryRepository = categoryRepository;
    }

    public List<CategoryData> execute(Boolean active, Long parentId) {
        return categoryRepository.findAll(active, parentId);
    }

    public List<CategoryData> executeTree(Boolean active) {
        return categoryRepository.findTree(active);
    }
}
