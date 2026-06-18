package com.nitrotech.api.infrastructure.persistence.spec;

import com.nitrotech.api.domain.order.dto.OrderFilter;
import com.nitrotech.api.infrastructure.persistence.entity.OrderEntity;
import com.nitrotech.api.infrastructure.persistence.entity.UserEntity;
import jakarta.persistence.criteria.Predicate;
import jakarta.persistence.criteria.Subquery;
import org.springframework.data.jpa.domain.Specification;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

public class OrderSpecification {

    public static Specification<OrderEntity> from(OrderFilter filter) {
        return Specification
                .where(notDeleted())
                .and(userId(filter.userId()))
                .and(status(filter.status()))
                .and(paymentMethod(filter.paymentMethod()))
                .and(createdFrom(filter.createdFrom()))
                .and(createdToExclusive(filter.createdToExclusive()))
                .and(amountRange(filter.amountMin(), filter.amountMax()))
                .and(search(filter.search()));
    }

    private static Specification<OrderEntity> notDeleted() {
        return (root, query, cb) -> cb.isNull(root.get("deletedAt"));
    }

    private static Specification<OrderEntity> userId(Long userId) {
        return (root, query, cb) -> userId == null ? cb.conjunction() : cb.equal(root.get("userId"), userId);
    }

    private static Specification<OrderEntity> status(String status) {
        return (root, query, cb) -> status == null ? cb.conjunction() : cb.equal(root.get("status"), status);
    }

    private static Specification<OrderEntity> paymentMethod(String paymentMethod) {
        return (root, query, cb) -> paymentMethod == null ? cb.conjunction() : cb.equal(root.get("paymentMethod"), paymentMethod);
    }

    private static Specification<OrderEntity> createdFrom(Instant createdFrom) {
        return (root, query, cb) -> createdFrom == null ? cb.conjunction() : cb.greaterThanOrEqualTo(root.get("createdAt"), createdFrom);
    }

    private static Specification<OrderEntity> createdToExclusive(Instant createdToExclusive) {
        return (root, query, cb) -> createdToExclusive == null ? cb.conjunction() : cb.lessThan(root.get("createdAt"), createdToExclusive);
    }

    private static Specification<OrderEntity> amountRange(BigDecimal amountMin, BigDecimal amountMax) {
        return (root, query, cb) -> {
            if (amountMin == null && amountMax == null) return cb.conjunction();
            List<Predicate> predicates = new ArrayList<>();
            if (amountMin != null) {
                predicates.add(cb.greaterThanOrEqualTo(root.get("finalAmount"), amountMin));
            }
            if (amountMax != null) {
                predicates.add(cb.lessThanOrEqualTo(root.get("finalAmount"), amountMax));
            }
            return cb.and(predicates.toArray(new Predicate[0]));
        };
    }

    private static Specification<OrderEntity> search(String search) {
        return (root, query, cb) -> {
            if (search == null || search.isBlank()) {
                return cb.conjunction();
            }
            String searchTrimmed = search.trim();
            String pattern = "%" + searchTrimmed.toLowerCase() + "%";

            List<Predicate> orPredicates = new ArrayList<>();

            // 1. Match order code format directly using the indexed orderCode column
            orPredicates.add(cb.like(cb.lower(root.get("orderCode")), pattern));

            // Also search by raw ID string if search query contains digits
            if (searchTrimmed.matches(".*\\d+.*")) {
                orPredicates.add(cb.like(cb.toString(root.get("id")), pattern));
            }

            // 2. ShippingAddress receiver/name
            var receiverPath = cb.function("jsonb_extract_path_text", String.class, root.get("shippingAddress"), cb.literal("receiver"));
            var namePath = cb.function("jsonb_extract_path_text", String.class, root.get("shippingAddress"), cb.literal("name"));
            orPredicates.add(cb.like(cb.lower(receiverPath), pattern));
            orPredicates.add(cb.like(cb.lower(namePath), pattern));

            // 3. ShippingAddress phone
            var phonePath = cb.function("jsonb_extract_path_text", String.class, root.get("shippingAddress"), cb.literal("phone"));
            orPredicates.add(cb.like(cb.lower(phonePath), pattern));

            // 4. User email subquery
            Subquery<Long> subquery = query.subquery(Long.class);
            var userRoot = subquery.from(UserEntity.class);
            subquery.select(userRoot.get("id"))
                    .where(cb.and(
                            cb.equal(userRoot.get("id"), root.get("userId")),
                            cb.like(cb.lower(userRoot.get("email")), pattern)
                    ));
            orPredicates.add(cb.exists(subquery));

            return cb.or(orPredicates.toArray(new Predicate[0]));
        };
    }
}
