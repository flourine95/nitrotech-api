package com.nitrotech.api.domain.category.usecase;

import com.nitrotech.api.domain.category.dto.*;
import com.nitrotech.api.domain.category.repository.CategoryRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class GetCategoriesUseCase {

    private final CategoryRepository categoryRepository;

    public CategoryPageResult execute(CategoryFilter filter, Pageable pageable) {
        Page<CategoryData> page = categoryRepository.findAll(filter, pageable);
        CategoryFacets facets = categoryRepository.getFacets();
        return new CategoryPageResult(page, facets);
    }

    public CategoryTreeResult executeTreeWithFacets(Boolean active) {
        List<CategoryData> tree = categoryRepository.findTree(active);
        CategoryFacets facets = categoryRepository.getFacets();
        return new CategoryTreeResult(tree, facets);
    }

    public List<CategoryData> executeTree(Boolean active) {
        return categoryRepository.findTree(active);
    }

    public List<CategoryData> executeDeleted() {
        return categoryRepository.findDeleted();
    }
}
