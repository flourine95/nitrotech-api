package com.nitrotech.api.domain.category.dto;

import java.time.LocalDateTime;
import java.util.List;

public record CategoryData(
        Long id,
        String name,
        String slug,
        String description,
        String image,
        Long parentId,
        String parentName,
        boolean active,
        int sortOrder,
        List<CategoryData> children,
        List<BreadcrumbItem> path,
        Integer childrenCount,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {
    // Backward-compatible constructor
    public CategoryData(Long id, String name, String slug, String description, String image,
                        Long parentId, String parentName, boolean active, int sortOrder,
                        List<CategoryData> children, LocalDateTime createdAt, LocalDateTime updatedAt) {
        this(id, name, slug, description, image, parentId, parentName, active, sortOrder,
                children, List.of(), children != null ? children.size() : 0, createdAt, updatedAt);
    }
}
