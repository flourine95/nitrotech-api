package com.nitrotech.api.infrastructure.persistence.repository;

import com.nitrotech.api.domain.category.dto.CategoryData;
import com.nitrotech.api.domain.category.dto.CategoryFilter;
import com.nitrotech.api.domain.category.dto.CreateCategoryCommand;
import com.nitrotech.api.domain.category.dto.MoveCategoryCommand;
import com.nitrotech.api.domain.category.dto.MoveCategoryResult;
import com.nitrotech.api.domain.category.dto.UpdateCategoryCommand;
import com.nitrotech.api.domain.category.repository.CategoryRepository;
import com.nitrotech.api.infrastructure.persistence.entity.CategoryEntity;
import com.nitrotech.api.infrastructure.persistence.spec.CategorySpecification;
import com.nitrotech.api.shared.exception.NotFoundException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

@Repository
public class CategoryRepositoryImpl implements CategoryRepository {

    private final CategoryJpaRepository jpa;

    public CategoryRepositoryImpl(CategoryJpaRepository jpa) {
        this.jpa = jpa;
    }

    @Override
    public CategoryData create(CreateCategoryCommand command) {
        CategoryEntity entity = new CategoryEntity();
        entity.setName(command.name());
        entity.setSlug(command.slug());
        entity.setDescription(command.description());
        entity.setImage(command.image());
        entity.setParentId(command.parentId());
        entity.setActive(command.active());
        return toData(jpa.save(entity), null, List.of());
    }

    @Override
    public CategoryData update(UpdateCategoryCommand command) {
        CategoryEntity entity = jpa.findActiveById(command.id())
                .orElseThrow(() -> new NotFoundException("CATEGORY_NOT_FOUND", "Category not found"));
        if (command.name() != null) entity.setName(command.name());
        if (command.slug() != null) entity.setSlug(command.slug());
        if (command.description() != null) entity.setDescription(command.description());
        if (command.image() != null) entity.setImage(command.image());
        if (command.parentId() != null) entity.setParentId(command.parentId());
        if (command.active() != null) entity.setActive(command.active());
        entity.setUpdatedAt(LocalDateTime.now());
        return toData(jpa.save(entity), null, List.of());
    }

    @Override
    public Optional<CategoryData> findById(Long id) {
        return jpa.findActiveById(id).map(e -> {
            String parentName = e.getParentId() != null
                    ? jpa.findActiveById(e.getParentId()).map(CategoryEntity::getName).orElse(null)
                    : null;
            return toData(e, parentName, List.of());
        });
    }

    @Override
    public Optional<CategoryData> findDeletedById(Long id) {
        return jpa.findDeletedById(id).map(e -> toData(e, null, List.of()));
    }

    @Override
    public Page<CategoryData> findAll(CategoryFilter filter, Pageable pageable) {
        return jpa.findAll(CategorySpecification.from(filter), pageable)
                .map(e -> toData(e, null, List.of()));
    }

    @Override
    public List<CategoryData> findTree(Boolean active) {
        List<CategoryEntity> all = jpa.findAllForTree(active);
        Map<Long, List<CategoryEntity>> byParent = all.stream()
                .filter(e -> e.getParentId() != null)
                .collect(Collectors.groupingBy(CategoryEntity::getParentId));

        return all.stream()
                .filter(e -> e.getParentId() == null)
                .map(e -> buildTree(e, byParent))
                .toList();
    }

    @Override
    public boolean existsById(Long id) {
        return jpa.existsActiveById(id);
    }

    @Override
    public boolean hasActiveChildren(Long id) {
        return jpa.existsActiveChildrenByParentId(id);
    }

    @Override
    public boolean hasAnyChildren(Long id) {
        return jpa.existsAnyChildrenByParentId(id);
    }

    @Override
    public boolean existsActiveBySlugAndIdNot(String slug, Long excludeId) {
        return jpa.existsActiveBySlugAndIdNot(slug, excludeId);
    }

    @Override
    public boolean existsBySlug(String slug) {
        return jpa.existsActiveBySlug(slug);
    }

    @Override
    public boolean existsBySlugAndIdNot(String slug, Long id) {
        return jpa.existsActiveBySlugAndIdNot(slug, id);
    }

    @Override
    public boolean isDescendantOf(Long potentialDescendantId, Long ancestorId) {
        // Traverse up the tree from potentialDescendantId
        Long current = potentialDescendantId;
        int maxDepth = 20; // guard against infinite loop
        while (current != null && maxDepth-- > 0) {
            if (current.equals(ancestorId)) return true;
            current = jpa.findActiveById(current).map(CategoryEntity::getParentId).orElse(null);
        }
        return false;
    }

    @Override
    public void softDelete(Long id) {
        jpa.findActiveById(id).ifPresent(e -> {
            e.setDeletedAt(LocalDateTime.now());
            jpa.save(e);
        });
    }

    @Override
    public void restore(Long id) {
        jpa.findDeletedById(id).ifPresent(e -> {
            e.setDeletedAt(null);
            jpa.save(e);
        });
    }

    @Override
    public void hardDelete(Long id) {
        jpa.deleteById(id);
    }

    @Override
    @org.springframework.transaction.annotation.Transactional
    public MoveCategoryResult moveCategory(MoveCategoryCommand command) {
        LocalDateTime now = LocalDateTime.now();
        List<CategoryData> updated = new java.util.ArrayList<>();

        // 1. Update parentId cho node được move
        CategoryEntity moved = jpa.findActiveById(command.movedId())
                .orElseThrow(() -> new NotFoundException("CATEGORY_NOT_FOUND", "Category not found"));
        moved.setParentId(command.toParentId());
        moved.setUpdatedAt(now);
        jpa.save(moved);

        // 2. Reindex target siblings (parent mới)
        for (int i = 0; i < command.targetOrderedIds().size(); i++) {
            Long siblingId = command.targetOrderedIds().get(i);
            jpa.findById(siblingId).ifPresent(e -> {
                e.setSortOrder(command.targetOrderedIds().indexOf(siblingId));
                e.setUpdatedAt(now);
                updated.add(toData(jpa.save(e), null, List.of()));
            });
        }

        // 3. Reindex source siblings (parent cũ) nếu cross-parent move
        boolean isCrossParent = !java.util.Objects.equals(command.fromParentId(), command.toParentId());
        if (isCrossParent && command.sourceOrderedIds() != null) {
            for (int i = 0; i < command.sourceOrderedIds().size(); i++) {
                final int sortOrder = i;
                Long siblingId = command.sourceOrderedIds().get(i);
                jpa.findById(siblingId).ifPresent(e -> {
                    e.setSortOrder(sortOrder);
                    e.setUpdatedAt(now);
                    updated.add(toData(jpa.save(e), null, List.of()));
                });
            }
        }

        return new MoveCategoryResult(updated);
    }

    private CategoryData buildTree(CategoryEntity entity, Map<Long, List<CategoryEntity>> byParent) {
        List<CategoryData> children = byParent.getOrDefault(entity.getId(), List.of())
                .stream()
                .map(child -> buildTree(child, byParent))
                .toList();
        return toData(entity, null, children);
    }

    private CategoryData toData(CategoryEntity e, String parentName, List<CategoryData> children) {
        return new CategoryData(
                e.getId(), e.getName(), e.getSlug(), e.getDescription(), e.getImage(),
                e.getParentId(), parentName, e.isActive(), e.getSortOrder(), children,
                e.getCreatedAt(), e.getUpdatedAt()
        );
    }
}
