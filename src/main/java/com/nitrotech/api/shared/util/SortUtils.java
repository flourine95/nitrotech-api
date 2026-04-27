package com.nitrotech.api.shared.util;

import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

public final class SortUtils {

    private SortUtils() {}

    /**
     * Parse multi-sort params thành Pageable.
     * Chỉ cho phép sort theo các field trong allowedFields để tránh injection.
     * Ví dụ: sort=name,asc&sort=createdAt,desc
     */
    public static Pageable toPageable(int page, int size, List<String> sortParams,
                                      Set<String> allowedFields, String defaultField) {
        int safePage = Math.max(0, page);
        int safeSize = Math.min(Math.max(1, size), 100);
        return PageRequest.of(safePage, safeSize, parseSort(sortParams, allowedFields, defaultField));
    }

    public static Sort parseSort(List<String> sortParams, Set<String> allowedFields, String defaultField) {
        if (sortParams == null || sortParams.isEmpty()) {
            return Sort.by(Sort.Direction.DESC, defaultField);
        }
        List<Sort.Order> orders = new ArrayList<>();
        for (String param : sortParams) {
            String[] parts = param.split(",", 2);
            String field = parts[0].trim();
            if (!allowedFields.contains(field)) continue;
            Sort.Direction dir = parts.length > 1 && parts[1].trim().equalsIgnoreCase("asc")
                    ? Sort.Direction.ASC : Sort.Direction.DESC;
            orders.add(new Sort.Order(dir, field));
        }
        return orders.isEmpty() ? Sort.by(Sort.Direction.DESC, defaultField) : Sort.by(orders);
    }
}
