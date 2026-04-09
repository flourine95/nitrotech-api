package com.nitrotech.api.domain.category.repository;

import com.nitrotech.api.domain.category.dto.CategoryData;
import com.nitrotech.api.domain.category.dto.CategoryFilter;
import com.nitrotech.api.domain.category.dto.CreateCategoryCommand;
import com.nitrotech.api.domain.category.dto.MoveCategoryCommand;
import com.nitrotech.api.domain.category.dto.MoveCategoryResult;
import com.nitrotech.api.domain.category.dto.UpdateCategoryCommand;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.List;
import java.util.Optional;

public interface CategoryRepository {
    CategoryData create(CreateCategoryCommand command);
    CategoryData update(UpdateCategoryCommand command);
    Optional<CategoryData> findById(Long id);
    Optional<CategoryData> findDeletedById(Long id);
    Page<CategoryData> findAll(CategoryFilter filter, Pageable pageable);
    List<CategoryData> findTree(Boolean active);
    boolean existsById(Long id);
    boolean hasActiveChildren(Long id);
    boolean hasAnyChildren(Long id);
    boolean existsActiveBySlugAndIdNot(String slug, Long excludeId);
    boolean existsBySlug(String slug);
    boolean existsBySlugAndIdNot(String slug, Long id);
    boolean isDescendantOf(Long potentialDescendantId, Long ancestorId);
    void softDelete(Long id);
    void restore(Long id);
    void hardDelete(Long id);
    MoveCategoryResult moveCategory(MoveCategoryCommand command);
}
