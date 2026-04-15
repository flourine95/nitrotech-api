package com.nitrotech.api.infrastructure.persistence.spec;

import com.nitrotech.api.domain.category.dto.CategoryFilter;
import com.nitrotech.api.infrastructure.persistence.entity.CategoryEntity;
import org.springframework.data.jpa.domain.Specification;

public class CategorySpecification {

    public static Specification<CategoryEntity> from(CategoryFilter filter) {
        return Specification
                .where(deleted(filter.deleted()))
                .and(active(filter.active()))
                .and(parentId(filter.parentId()))
                .and(search(filter.search()));
    }

    private static Specification<CategoryEntity> deleted(Boolean deleted) {
        return (root, query, cb) -> {
            if (deleted == null) return cb.conjunction();
            if (deleted) return cb.isNotNull(root.get("deletedAt"));
            return cb.isNull(root.get("deletedAt"));
        };
    }

    private static Specification<CategoryEntity> active(Boolean active) {
        return (root, query, cb) -> {
            if (active == null) return cb.conjunction();
            return cb.equal(root.get("active"), active);
        };
    }

    private static Specification<CategoryEntity> parentId(Long parentId) {
        return (root, query, cb) -> {
            if (parentId == null) return cb.conjunction();
            return cb.equal(root.get("parentId"), parentId);
        };
    }

    private static Specification<CategoryEntity> search(String search) {
        return (root, query, cb) -> {
            if (search == null || search.isBlank()) return cb.conjunction();
            String cleaned = search.replace("\"", "").trim();
            if (cleaned.isBlank()) return cb.conjunction();
            String pattern = "%" + cleaned.toLowerCase() + "%";
            return cb.or(
                    cb.like(cb.lower(root.get("name")), pattern),
                    cb.like(cb.lower(root.get("slug")), pattern)
            );
        };
    }
}
