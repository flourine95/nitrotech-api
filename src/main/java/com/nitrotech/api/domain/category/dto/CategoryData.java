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
        List<CategoryData> children,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {}
