package com.nitrotech.api.domain.category.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import io.swagger.v3.oas.annotations.media.Schema;

import java.time.Instant;
import java.util.List;

@Schema(description = "Category data with nested children and breadcrumb path")
public record CategoryData(
        @Schema(description = "Category ID", example = "1")
        Long id,

        @Schema(description = "Category name", example = "Electronics")
        String name,

        @Schema(description = "URL-friendly slug", example = "electronics")
        String slug,

        @Schema(description = "Category description", example = "All electronic devices")
        String description,

        @Schema(description = "Category image URL", example = "https://example.com/electronics.jpg")
        String image,

        @Schema(description = "Parent category ID (null for root)", example = "null")
        Long parentId,

        @Schema(description = "Parent category name", example = "null")
        String parentName,

        @Schema(description = "Whether category is active", example = "true")
        boolean active,

        @Schema(description = "Sort order within parent", example = "1")
        int sortOrder,

        @Schema(description = "Child categories (recursive structure)")
        List<CategoryData> children,

        @Schema(description = "Breadcrumb path from root to this category")
        @JsonInclude(JsonInclude.Include.NON_NULL)
        List<BreadcrumbItem> path,

        @Schema(description = "Number of direct children", example = "5")
        Integer childrenCount,

        @Schema(description = "Number of products in this category", example = "23")
        Integer productCount,

        @Schema(description = "Creation timestamp", example = "2026-05-08T10:30:00Z")
        Instant createdAt,

        @Schema(description = "Last update timestamp", example = "2026-05-08T15:45:00Z")
        Instant updatedAt
) {
}
