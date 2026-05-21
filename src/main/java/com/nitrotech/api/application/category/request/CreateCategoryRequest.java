package com.nitrotech.api.application.category.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

@Schema(description = "Request body for creating a new category")
public record CreateCategoryRequest(
        @Schema(description = "Category name", example = "Electronics", requiredMode = Schema.RequiredMode.REQUIRED)
        @NotBlank(message = "Name is required")
        @Size(max = 255, message = "Name must be at most 255 characters")
        String name,

        @Schema(description = "URL-friendly slug (lowercase, hyphens only)", example = "electronics", requiredMode = Schema.RequiredMode.REQUIRED)
        @NotBlank(message = "Slug is required")
        @Pattern(regexp = "^[a-z0-9]+(?:-[a-z0-9]+)*$", message = "Slug must be lowercase letters, numbers and hyphens")
        @Size(max = 255, message = "Slug must be at most 255 characters")
        String slug,

        @Schema(description = "Category description", example = "All electronic devices and accessories")
        String description,
        
        @Schema(description = "Category image URL", example = "https://example.com/electronics.jpg")
        String image,
        
        @Schema(description = "Parent category ID (null for root category)", example = "1")
        Long parentId,
        
        @Schema(description = "Whether the category is active", example = "true", defaultValue = "true")
        boolean active
) {}
