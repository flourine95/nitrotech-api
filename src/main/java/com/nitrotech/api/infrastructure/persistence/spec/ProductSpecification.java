package com.nitrotech.api.infrastructure.persistence.spec;

import com.nitrotech.api.domain.product.dto.ProductFilter;
import com.nitrotech.api.infrastructure.persistence.entity.ProductEntity;
import org.springframework.data.jpa.domain.Specification;

public class ProductSpecification {

    public static Specification<ProductEntity> from(ProductFilter filter) {
        return Specification
                .where(deleted(filter.deleted()))
                .and(active(filter.active()))
                .and(categoryId(filter.categoryId()))
                .and(brandId(filter.brandId()))
                .and(search(filter.search()));
    }

    private static Specification<ProductEntity> deleted(Boolean deleted) {
        return (root, query, cb) -> {
            if (deleted == null) return cb.conjunction();
            if (deleted) return cb.isNotNull(root.get("deletedAt"));
            return cb.isNull(root.get("deletedAt"));
        };
    }

    private static Specification<ProductEntity> active(Boolean active) {
        return (root, query, cb) -> {
            if (active == null) return cb.conjunction();
            return cb.equal(root.get("active"), active);
        };
    }

    private static Specification<ProductEntity> categoryId(Long categoryId) {
        return (root, query, cb) -> {
            if (categoryId == null) return cb.conjunction();
            return cb.equal(root.get("categoryId"), categoryId);
        };
    }

    private static Specification<ProductEntity> brandId(Long brandId) {
        return (root, query, cb) -> {
            if (brandId == null) return cb.conjunction();
            return cb.equal(root.get("brandId"), brandId);
        };
    }

    private static Specification<ProductEntity> search(String search) {
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
