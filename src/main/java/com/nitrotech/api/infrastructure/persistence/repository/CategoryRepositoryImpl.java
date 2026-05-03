package com.nitrotech.api.infrastructure.persistence.repository;

import com.nitrotech.api.domain.category.dto.*;
import com.nitrotech.api.domain.category.repository.CategoryRepository;
import com.nitrotech.api.infrastructure.persistence.entity.CategoryEntity;
import com.nitrotech.api.infrastructure.persistence.spec.CategorySpecification;
import com.nitrotech.api.shared.exception.ConflictException;
import com.nitrotech.api.shared.exception.NotFoundException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
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
        return toData(jpa.save(entity), null, List.of(), false);  // Don't load breadcrumb
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
        return toData(jpa.save(entity), null, List.of(), false);  // Don't load breadcrumb
    }

    @Override
    public Optional<CategoryData> findById(Long id) {
        return jpa.findActiveById(id).map(e -> {
            String parentName = e.getParentId() != null
                    ? jpa.findActiveById(e.getParentId()).map(CategoryEntity::getName).orElse(null)
                    : null;
            int productCount = jpa.countProductsByCategoryId(id);
            return toData(e, parentName, List.of(), true, productCount);  // Load breadcrumb and productCount
        });
    }

    @Override
    public Optional<CategoryData> findDeletedById(Long id) {
        int productCount = jpa.countProductsByCategoryId(id);
        return jpa.findDeletedById(id).map(e -> toData(e, null, List.of(), false, productCount));
    }

    @Override
    public Page<CategoryData> findAll(CategoryFilter filter, Pageable pageable) {
        return jpa.findAll(CategorySpecification.from(filter), pageable)
                .map(e -> toData(e, null, List.of(), false));  // Don't load breadcrumb for list
    }

    @Override
    public List<CategoryData> findTree(Boolean active) {
        List<CategoryEntity> all = jpa.findAllForTree(active);

        // Load product counts for all categories in one query
        Map<Long, Integer> productCounts = jpa.getProductCountsForAllCategories().stream()
                .collect(Collectors.toMap(
                        row -> ((Number) row[0]).longValue(),
                        row -> ((Number) row[1]).intValue()
                ));

        Map<Long, List<CategoryEntity>> byParent = all.stream()
                .filter(e -> e.getParentId() != null)
                .collect(Collectors.groupingBy(CategoryEntity::getParentId));

        return all.stream()
                .filter(e -> e.getParentId() == null)
                .map(e -> buildTree(e, byParent, productCounts))
                .toList();
    }

    @Override
    public List<CategoryData> findDeleted() {
        return jpa.findAllDeleted().stream()
                .map(e -> {
                    String parentName = e.getParentId() != null
                            ? jpa.findById(e.getParentId()).map(CategoryEntity::getName).orElse(null)
                            : null;
                    return toData(e, parentName, List.of(), false);  // Don't load breadcrumb for list
                })
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
    @Transactional
    public MoveCategoryResult moveCategory(MoveCategoryCommand command) {
        LocalDateTime now = LocalDateTime.now();
        List<CategoryData> updated = new ArrayList<>();

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
        boolean isCrossParent = !Objects.equals(command.fromParentId(), command.toParentId());
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

    @Override
    public CategoryFacets getFacets() {
        List<Object[]> results = jpa.getFacets();

        // Handle case where result might be null or empty
        if (results == null || results.isEmpty()) {
            return new CategoryFacets(0L, 0L, 0L, 0L, 0L);
        }

        Object[] result = results.getFirst();

        return new CategoryFacets(
                result[0] != null ? ((Number) result[0]).longValue() : 0L,  // active
                result[1] != null ? ((Number) result[1]).longValue() : 0L,  // inactive
                result[2] != null ? ((Number) result[2]).longValue() : 0L,  // deleted
                result[3] != null ? ((Number) result[3]).longValue() : 0L,  // root
                result[4] != null ? ((Number) result[4]).longValue() : 0L   // withChildren
        );
    }

    private CategoryData buildTree(CategoryEntity entity, Map<Long, List<CategoryEntity>> byParent, Map<Long, Integer> productCounts) {
        List<CategoryData> children = byParent.getOrDefault(entity.getId(), List.of())
                .stream()
                .map(child -> buildTree(child, byParent, productCounts))
                .toList();
        int productCount = productCounts.getOrDefault(entity.getId(), 0);
        return toData(entity, null, children, false, productCount);  // Don't load breadcrumb for tree
    }

    private CategoryData toData(CategoryEntity e, String parentName, List<CategoryData> children) {
        return toData(e, parentName, children, true, 0);  // Load breadcrumb by default, productCount = 0
    }

    private CategoryData toData(CategoryEntity e, String parentName, List<CategoryData> children, boolean loadBreadcrumb) {
        return toData(e, parentName, children, loadBreadcrumb, 0);  // productCount = 0
    }

    private CategoryData toData(CategoryEntity e, String parentName, List<CategoryData> children, boolean loadBreadcrumb, int productCount) {
        // Get breadcrumb path only if requested
        List<BreadcrumbItem> path = loadBreadcrumb
                ? jpa.findPath(e.getId()).stream()
                  .map(row -> new BreadcrumbItem(
                          ((Number) row[0]).longValue(),
                          (String) row[1],
                          (String) row[2],
                          (Boolean) row[3]
                  ))
                  .toList()
                : null;  // Return null instead of empty list

        return new CategoryData(
                e.getId(), e.getName(), e.getSlug(), e.getDescription(), e.getImage(),
                e.getParentId(), parentName, e.isActive(), e.getSortOrder(), children,
                path, children != null ? children.size() : 0, productCount,
                e.getCreatedAt(), e.getUpdatedAt()
        );
    }

    @Override
    @Transactional
    public List<Long> bulkSoftDelete(List<Long> ids) {
        // Only delete categories that have no children
        List<Long> deletableIds = new ArrayList<>();
        for (Long id : ids) {
            if (jpa.existsActiveById(id) && !jpa.existsAnyChildrenByParentId(id)) {
                deletableIds.add(id);
            }
        }
        if (!deletableIds.isEmpty()) {
            jpa.bulkSoftDelete(deletableIds, LocalDateTime.now());
        }
        return deletableIds;
    }

    @Override
    @Transactional
    public List<Long> bulkRestore(List<Long> ids) {
        List<Long> restorableIds = jpa.findAllDeletedByIds(ids).stream()
                .map(CategoryEntity::getId).toList();
        if (!restorableIds.isEmpty()) {
            jpa.bulkRestore(restorableIds);
        }
        return restorableIds;
    }

    @Override
    @Transactional
    public List<Long> bulkHardDelete(List<Long> ids) {
        // Only hard delete categories that are already soft deleted and have no children
        List<Long> deletableIds = new ArrayList<>();
        for (Long id : ids) {
            Optional<CategoryEntity> entity = jpa.findDeletedById(id);
            if (entity.isPresent() && !jpa.existsAnyChildrenByParentId(id)) {
                deletableIds.add(id);
            }
        }
        if (!deletableIds.isEmpty()) {
            jpa.deleteAllById(deletableIds);
        }
        return deletableIds;
    }

    @Override
    @Transactional
    public List<Long> bulkActivate(List<Long> ids) {
        List<Long> activatableIds = jpa.findAllActiveByIds(ids).stream()
                .map(CategoryEntity::getId).toList();
        if (!activatableIds.isEmpty()) {
            jpa.bulkActivate(activatableIds);
        }
        return activatableIds;
    }

    @Override
    @Transactional
    public List<Long> bulkDeactivate(List<Long> ids) {
        List<Long> deactivatableIds = jpa.findAllActiveByIds(ids).stream()
                .map(CategoryEntity::getId).toList();
        if (!deactivatableIds.isEmpty()) {
            jpa.bulkDeactivate(deactivatableIds);
        }
        return deactivatableIds;
    }

    @Override
    @Transactional
    public CategoryData moveUp(Long id) {
        CategoryEntity entity = jpa.findActiveById(id)
                .orElseThrow(() -> new NotFoundException("CATEGORY_NOT_FOUND", "Category not found"));

        // Find all siblings (same parent), sorted by sortOrder then id
        List<CategoryEntity> siblings = jpa.findAllActive(null, entity.getParentId());
        siblings.sort((a, b) -> {
            int cmp = Integer.compare(a.getSortOrder(), b.getSortOrder());
            return cmp != 0 ? cmp : Long.compare(a.getId(), b.getId());
        });

        // Find current position
        int currentIndex = -1;
        for (int i = 0; i < siblings.size(); i++) {
            if (siblings.get(i).getId().equals(id)) {
                currentIndex = i;
                break;
            }
        }

        if (currentIndex <= 0) {
            throw new ConflictException(
                    "ALREADY_FIRST", "Category is already at the first position");
        }

        // Swap positions in list
        CategoryEntity temp = siblings.get(currentIndex);
        siblings.set(currentIndex, siblings.get(currentIndex - 1));
        siblings.set(currentIndex - 1, temp);

        // Reindex sortOrder to ensure uniqueness
        LocalDateTime now = LocalDateTime.now();
        for (int i = 0; i < siblings.size(); i++) {
            siblings.get(i).setSortOrder(i);
            siblings.get(i).setUpdatedAt(now);
            jpa.save(siblings.get(i));
        }

        return toData(entity, null, List.of(), false);  // Don't load breadcrumb
    }

    @Override
    @Transactional
    public CategoryData moveDown(Long id) {
        CategoryEntity entity = jpa.findActiveById(id)
                .orElseThrow(() -> new NotFoundException("CATEGORY_NOT_FOUND", "Category not found"));

        // Find all siblings (same parent), sorted by sortOrder then id
        List<CategoryEntity> siblings = jpa.findAllActive(null, entity.getParentId());
        siblings.sort((a, b) -> {
            int cmp = Integer.compare(a.getSortOrder(), b.getSortOrder());
            return cmp != 0 ? cmp : Long.compare(a.getId(), b.getId());
        });

        // Find current position
        int currentIndex = -1;
        for (int i = 0; i < siblings.size(); i++) {
            if (siblings.get(i).getId().equals(id)) {
                currentIndex = i;
                break;
            }
        }

        if (currentIndex < 0 || currentIndex >= siblings.size() - 1) {
            throw new ConflictException(
                    "ALREADY_LAST", "Category is already at the last position");
        }

        // Swap positions in list
        CategoryEntity temp = siblings.get(currentIndex);
        siblings.set(currentIndex, siblings.get(currentIndex + 1));
        siblings.set(currentIndex + 1, temp);

        // Reindex sortOrder to ensure uniqueness
        LocalDateTime now = LocalDateTime.now();
        for (int i = 0; i < siblings.size(); i++) {
            siblings.get(i).setSortOrder(i);
            siblings.get(i).setUpdatedAt(now);
            jpa.save(siblings.get(i));
        }

        return toData(entity, null, List.of(), false);  // Don't load breadcrumb
    }

    @Override
    @Transactional
    public CategoryData move(Long id, Long newParentId, Long afterId) {
        CategoryEntity entity = jpa.findActiveById(id)
                .orElseThrow(() -> new NotFoundException("CATEGORY_NOT_FOUND", "Category not found"));

        LocalDateTime now = LocalDateTime.now();

        // Validate circular reference if changing parent
        if (newParentId != null && !newParentId.equals(entity.getParentId())) {
            if (isDescendantOf(newParentId, id)) {
                throw new ConflictException(
                        "CIRCULAR_REFERENCE", "Cannot move category into its own descendant");
            }
            entity.setParentId(newParentId);
        }

        // Calculate sortOrder
        if (afterId != null) {
            // Place after specific category
            CategoryEntity afterCategory = jpa.findActiveById(afterId)
                    .orElseThrow(() -> new ConflictException(
                            "INVALID_AFTER_ID", "afterId category not found"));

            entity.setSortOrder(afterCategory.getSortOrder() + 1);

            // Shift categories after
            List<CategoryEntity> toShift = jpa.findAllActive(null, entity.getParentId()).stream()
                    .filter(c -> !c.getId().equals(id) && c.getSortOrder() > afterCategory.getSortOrder())
                    .toList();
            for (CategoryEntity c : toShift) {
                c.setSortOrder(c.getSortOrder() + 1);
                c.setUpdatedAt(now);
                jpa.save(c);
            }
        } else {
            // Place at the end
            List<CategoryEntity> siblings = jpa.findAllActive(null, entity.getParentId());
            int maxOrder = siblings.stream()
                    .filter(c -> !c.getId().equals(id))
                    .mapToInt(CategoryEntity::getSortOrder)
                    .max()
                    .orElse(-1);
            entity.setSortOrder(maxOrder + 1);
        }

        entity.setUpdatedAt(now);
        return toData(jpa.save(entity), null, List.of(), false);  // Don't load breadcrumb
    }
}