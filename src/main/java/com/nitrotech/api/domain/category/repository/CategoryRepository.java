package com.nitrotech.api.domain.category.repository;

import com.nitrotech.api.domain.category.dto.*;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.List;
import java.util.Optional;

public interface CategoryRepository {
    CategoryData create(CreateCategoryCommand command);

    CategoryData update(UpdateCategoryCommand command);

    Optional<CategoryData> findNotDeletedById(Long id);

    Optional<CategoryData> findNotDeletedBySlug(String slug);

    Optional<CategoryData> findVisibleById(Long id);

    Optional<CategoryData> findVisibleBySlug(String slug);

    Optional<CategoryData> findDeletedById(Long id);

    Page<CategoryData> findAll(CategoryFilter filter, Pageable pageable);

    List<CategoryData> findTree(Boolean active);

    List<CategoryData> findDeleted();

    boolean existsById(Long id);

    boolean hasNotDeletedChildren(Long id);

    boolean hasAnyChildren(Long id);

    boolean existsNotDeletedBySlug(String slug);

    boolean existsNotDeletedBySlugAndIdNot(String slug, Long excludeId);

    boolean isDescendantOf(Long potentialDescendantId, Long ancestorId);

    void softDelete(Long id);

    void restore(Long id);

    void hardDelete(Long id);

    MoveCategoryResult moveCategory(MoveCategoryCommand command);

    // Bulk operations
    List<Long> bulkSoftDelete(List<Long> ids);

    List<Long> bulkRestore(List<Long> ids);

    List<Long> bulkHardDelete(List<Long> ids);

    List<Long> bulkActivate(List<Long> ids);

    List<Long> bulkDeactivate(List<Long> ids);

    // Move operations
    CategoryData moveUp(Long id);

    CategoryData moveDown(Long id);

    CategoryData move(Long id, Long newParentId, Long afterId);

    // Facets
    CategoryFacets getFacets();
}
