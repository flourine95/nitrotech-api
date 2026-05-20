package com.nitrotech.api.infrastructure.persistence.spec;

import com.nitrotech.api.domain.product.dto.ProductFilter;
import com.nitrotech.api.infrastructure.persistence.entity.ProductEntity;
import com.nitrotech.api.infrastructure.persistence.entity.ProductVariantEntity;
import jakarta.persistence.criteria.Subquery;
import org.springframework.data.jpa.domain.Specification;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

public class ProductSpecification {

    public static Specification<ProductEntity> from(ProductFilter filter) {
        return Specification
                .where(deleted(filter.deleted()))
                .and(active(filter.active()))
                .and(categoryId(filter.categoryId()))
                .and(brandId(filter.brandId()))
                .and(categoryIds(filter.categoryIds()))
                .and(brandIds(filter.brandIds()))
                .and(priceRange(filter.minPrice(), filter.maxPrice()))
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

    private static Specification<ProductEntity> categoryIds(List<Long> categoryIds) {
        return (root, query, cb) -> {
            if (categoryIds == null || categoryIds.isEmpty()) return cb.conjunction();
            return root.get("categoryId").in(categoryIds);
        };
    }

    private static Specification<ProductEntity> brandIds(List<Long> brandIds) {
        return (root, query, cb) -> {
            if (brandIds == null || brandIds.isEmpty()) return cb.conjunction();
            return root.get("brandId").in(brandIds);
        };
    }

    private static Specification<ProductEntity> priceRange(BigDecimal minPrice, BigDecimal maxPrice) {
        return (root, query, cb) -> {
            if (minPrice == null && maxPrice == null) return cb.conjunction();

            // Subquery to check if product has any variant within price range
            Subquery<Long> subquery = query.subquery(Long.class);
            var variantRoot = subquery.from(ProductVariantEntity.class);

            List<jakarta.persistence.criteria.Predicate> predicates = new ArrayList<>();
            predicates.add(cb.equal(variantRoot.get("productId"), root.get("id")));
            predicates.add(cb.isTrue(variantRoot.get("active")));
            predicates.add(cb.isNull(variantRoot.get("deletedAt")));

            if (minPrice != null) {
                predicates.add(cb.greaterThanOrEqualTo(variantRoot.get("price"), minPrice));
            }
            if (maxPrice != null) {
                predicates.add(cb.lessThanOrEqualTo(variantRoot.get("price"), maxPrice));
            }

            subquery.select(variantRoot.get("id"))
                    .where(predicates.toArray(new jakarta.persistence.criteria.Predicate[0]));

            return cb.exists(subquery);
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
