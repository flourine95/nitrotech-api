package com.nitrotech.api.infrastructure.persistence.spec;

import com.nitrotech.api.domain.brand.dto.BrandFilter;
import com.nitrotech.api.infrastructure.persistence.entity.BrandEntity;
import org.springframework.data.jpa.domain.Specification;

public class BrandSpecification {

    public static Specification<BrandEntity> from(BrandFilter filter) {
        return Specification
                .where(deleted(filter.deleted()))
                .and(active(filter.active()))
                .and(search(filter.search()));
    }

    private static Specification<BrandEntity> deleted(Boolean deleted) {
        return (root, query, cb) -> {
            if (deleted == null) return cb.conjunction(); // tất cả
            if (deleted) return cb.isNotNull(root.get("deletedAt")); // chỉ deleted
            return cb.isNull(root.get("deletedAt")); // chưa deleted
        };
    }

    private static Specification<BrandEntity> active(Boolean active) {
        return (root, query, cb) -> {
            if (active == null) return cb.conjunction();
            return cb.equal(root.get("active"), active);
        };
    }

    private static Specification<BrandEntity> search(String search) {
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
