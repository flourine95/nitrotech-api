package com.nitrotech.api.domain.category.repository;

import com.nitrotech.api.domain.category.dto.CategoryData;
import com.nitrotech.api.domain.category.dto.CreateCategoryCommand;
import com.nitrotech.api.domain.category.dto.UpdateCategoryCommand;

import java.util.List;
import java.util.Optional;

public interface CategoryRepository {
    CategoryData create(CreateCategoryCommand command);
    CategoryData update(UpdateCategoryCommand command);
    Optional<CategoryData> findById(Long id);
    List<CategoryData> findAll(Boolean active, Long parentId);
    List<CategoryData> findTree(Boolean active);
    boolean existsById(Long id);
    boolean existsBySlug(String slug);
    boolean existsBySlugAndIdNot(String slug, Long id);
    boolean isDescendantOf(Long potentialDescendantId, Long ancestorId);
    void softDelete(Long id);
}
