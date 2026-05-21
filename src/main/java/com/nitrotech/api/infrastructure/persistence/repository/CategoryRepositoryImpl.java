package com.nitrotech.api.infrastructure.persistence.repository;

import com.nitrotech.api.domain.category.dto.*;
import com.nitrotech.api.domain.category.repository.CategoryRepository;
import com.nitrotech.api.infrastructure.persistence.entity.CategoryEntity;
import com.nitrotech.api.infrastructure.persistence.spec.CategorySpecification;
import com.nitrotech.api.shared.exception.ConflictException;
import com.nitrotech.api.shared.exception.NotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

@Repository
@RequiredArgsConstructor
public class CategoryRepositoryImpl implements CategoryRepository {

    private final CategoryJpaRepository jpa;

    @Override
    @Transactional
    public CategoryData create(CreateCategoryCommand command) {
        CategoryEntity entity = new CategoryEntity();
        entity.setName(command.name());
        entity.setSlug(command.slug());
        entity.setDescription(command.description());
        entity.setImage(command.image());
        entity.setParentId(command.parentId());
        entity.setActive(command.active());
        
        // Set sortOrder to end of siblings list
        List<CategoryEntity> siblings = jpa.findAllActive(null, command.parentId());
        int maxOrder = siblings.stream()
                .mapToInt(CategoryEntity::getSortOrder)
                .max()
                .orElse(-1);
        entity.setSortOrder(maxOrder + 1);
        
        return toDataSimple(jpa.save(entity));
    }

    @Override
    public CategoryData update(UpdateCategoryCommand command) {
        CategoryEntity entity = jpa.findActiveById(command.id())
                .orElseThrow(() -> new NotFoundException("CATEGORY_NOT_FOUND", 
                        "Category with ID " + command.id() + " not found"));
        if (command.name() != null) entity.setName(command.name());
        if (command.slug() != null) entity.setSlug(command.slug());
        if (command.description() != null) entity.setDescription(command.description());
        if (command.image() != null) entity.setImage(command.image());
        if (command.parentId() != null) entity.setParentId(command.parentId());
        if (command.active() != null) entity.setActive(command.active());
        return toDataSimple(jpa.save(entity));
    }

    @Override
    public Optional<CategoryData> findById(Long id) {
        return jpa.findActiveById(id).map(this::toDataForDetail);
    }

    @Override
    public Optional<CategoryData> findBySlug(String slug) {
        return jpa.findBySlugAndDeletedAtIsNull(slug).map(this::toDataForDetail);
    }

    @Override
    public Optional<CategoryData> findDeletedById(Long id) {
        int productCount = jpa.countProductsByCategoryId(id);
        return jpa.findDeletedById(id).map(e -> toDataForDeleted(e, productCount));
    }

    @Override
    public Page<CategoryData> findAll(CategoryFilter filter, Pageable pageable) {
        return jpa.findAll(CategorySpecification.from(filter), pageable)
                .map(this::toDataSimple);
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
                .map(e -> toDataForDeleted(e, 0))
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
    @Transactional
    public void softDelete(Long id) {
        jpa.findActiveById(id).ifPresent(e -> {
            Long parentId = e.getParentId();
            int deletedSortOrder = e.getSortOrder();
            
            // Soft delete the category
            e.setDeletedAt(LocalDateTime.now());
            jpa.save(e);
            
            // Reindex siblings after the deleted category
            List<CategoryEntity> siblings = jpa.findAllActive(null, parentId);
            siblings.stream()
                    .filter(sibling -> sibling.getSortOrder() > deletedSortOrder)
                    .forEach(sibling -> {
                        sibling.setSortOrder(sibling.getSortOrder() - 1);
                        jpa.save(sibling);
                    });
        });
    }

    @Override
    @Transactional
    public void restore(Long id) {
        jpa.findDeletedById(id).ifPresent(e -> {
            e.setDeletedAt(null);
            
            // Set sortOrder to end of siblings list
            List<CategoryEntity> siblings = jpa.findAllActive(null, e.getParentId());
            int maxOrder = siblings.stream()
                    .mapToInt(CategoryEntity::getSortOrder)
                    .max()
                    .orElse(-1);
            e.setSortOrder(maxOrder + 1);
            
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
        CategoryEntity moved = jpa.findActiveById(command.movedId())
                .orElseThrow(() -> new NotFoundException("CATEGORY_NOT_FOUND", 
                        "Category with ID " + command.movedId() + " not found"));
        
        List<CategoryData> updated = new ArrayList<>();

        // 1. Update parentId cho node được move
        moved.setParentId(command.toParentId());
        jpa.save(moved);

        // 2. Reindex target siblings (parent mới)
        for (int i = 0; i < command.targetOrderedIds().size(); i++) {
            Long siblingId = command.targetOrderedIds().get(i);
            jpa.findById(siblingId).ifPresent(e -> {
                e.setSortOrder(command.targetOrderedIds().indexOf(siblingId));
                updated.add(toDataSimple(jpa.save(e)));
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
                    updated.add(toDataSimple(jpa.save(e)));
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
        return toDataForTree(entity, children, productCount);
    }

    /**
     * Simple conversion for list, move, and bulk operations.
     * No breadcrumb, no product count.
     */
    private CategoryData toDataSimple(CategoryEntity entity) {
        return new CategoryData(
                entity.getId(), entity.getName(), entity.getSlug(),
                entity.getDescription(), entity.getImage(),
                entity.getParentId(), null, entity.isActive(),
                entity.getSortOrder(), List.of(),
                null, 0, 0,
                entity.getCreatedAt(), entity.getUpdatedAt()
        );
    }

    /**
     * Detailed conversion for get by id operations.
     * Includes breadcrumb and product count.
     */
    private CategoryData toDataForDetail(CategoryEntity entity) {
        List<BreadcrumbItem> path = jpa.findPath(entity.getId()).stream()
                .map(row -> new BreadcrumbItem(
                        ((Number) row[0]).longValue(),
                        (String) row[1],
                        (String) row[2],
                        (Boolean) row[3]
                ))
                .toList();

        String parentName = entity.getParentId() != null
                ? jpa.findById(entity.getParentId()).map(CategoryEntity::getName).orElse(null)
                : null;

        int productCount = jpa.countProductsByCategoryId(entity.getId());

        return new CategoryData(
                entity.getId(), entity.getName(), entity.getSlug(),
                entity.getDescription(), entity.getImage(),
                entity.getParentId(), parentName, entity.isActive(),
                entity.getSortOrder(), List.of(),
                path, 0, productCount,
                entity.getCreatedAt(), entity.getUpdatedAt()
        );
    }

    /**
     * Tree conversion with children and product count.
     * No breadcrumb (not needed for tree view).
     */
    private CategoryData toDataForTree(CategoryEntity entity, List<CategoryData> children, int productCount) {
        return new CategoryData(
                entity.getId(), entity.getName(), entity.getSlug(),
                entity.getDescription(), entity.getImage(),
                entity.getParentId(), null, entity.isActive(),
                entity.getSortOrder(), children,
                null, children.size(), productCount,
                entity.getCreatedAt(), entity.getUpdatedAt()
        );
    }

    /**
     * Conversion for deleted items with product count.
     * No breadcrumb, includes parent name.
     */
    private CategoryData toDataForDeleted(CategoryEntity entity, int productCount) {
        String parentName = entity.getParentId() != null
                ? jpa.findById(entity.getParentId()).map(CategoryEntity::getName).orElse(null)
                : null;

        return new CategoryData(
                entity.getId(), entity.getName(), entity.getSlug(),
                entity.getDescription(), entity.getImage(),
                entity.getParentId(), parentName, entity.isActive(),
                entity.getSortOrder(), List.of(),
                null, 0, productCount,
                entity.getCreatedAt(), entity.getUpdatedAt()
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
                .orElseThrow(() -> new NotFoundException("CATEGORY_NOT_FOUND", 
                        "Category with ID " + id + " not found"));

        List<CategoryEntity> siblings = jpa.findAllActive(null, entity.getParentId());
        siblings.sort(Comparator.comparingInt(CategoryEntity::getSortOrder).thenComparingLong(CategoryEntity::getId));

        int currentIndex = IntStream.range(0, siblings.size())
                .filter(i -> siblings.get(i).getId().equals(id))
                .findFirst()
                .orElse(-1);

        if (currentIndex <= 0) {
            throw new ConflictException("ALREADY_FIRST", "Category is already at the first position");
        }

        CategoryEntity temp = siblings.get(currentIndex);
        siblings.set(currentIndex, siblings.get(currentIndex - 1));
        siblings.set(currentIndex - 1, temp);

        for (int i = 0; i < siblings.size(); i++) {
            siblings.get(i).setSortOrder(i);
            jpa.save(siblings.get(i));
        }

        return toDataSimple(entity);
    }

    @Override
    @Transactional
    public CategoryData moveDown(Long id) {
        CategoryEntity entity = jpa.findActiveById(id)
                .orElseThrow(() -> new NotFoundException("CATEGORY_NOT_FOUND", 
                        "Category with ID " + id + " not found"));

        List<CategoryEntity> siblings = jpa.findAllActive(null, entity.getParentId());
        siblings.sort(Comparator.comparingInt(CategoryEntity::getSortOrder).thenComparingLong(CategoryEntity::getId));

        int currentIndex = IntStream.range(0, siblings.size())
                .filter(i -> siblings.get(i).getId().equals(id))
                .findFirst()
                .orElse(-1);

        if (currentIndex < 0 || currentIndex >= siblings.size() - 1) {
            throw new ConflictException("ALREADY_LAST", "Category is already at the last position");
        }

        CategoryEntity temp = siblings.get(currentIndex);
        siblings.set(currentIndex, siblings.get(currentIndex + 1));
        siblings.set(currentIndex + 1, temp);

        for (int i = 0; i < siblings.size(); i++) {
            siblings.get(i).setSortOrder(i);
            jpa.save(siblings.get(i));
        }

        return toDataSimple(entity);
    }

    @Override
    @Transactional
    public CategoryData move(Long id, Long newParentId, Long afterId) {
        CategoryEntity entity = jpa.findActiveById(id)
                .orElseThrow(() -> new NotFoundException("CATEGORY_NOT_FOUND", 
                        "Category with ID " + id + " not found"));

        // Validate circular reference if changing parent
        if (!Objects.equals(newParentId, entity.getParentId())) {
            // Validate circular reference only if newParentId is not null
            if (newParentId != null && isDescendantOf(newParentId, id)) {
                throw new ConflictException(
                        "CATEGORY_CIRCULAR_REF", "Cannot move category into its own descendant");
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

        return toDataSimple(jpa.save(entity));
    }
}