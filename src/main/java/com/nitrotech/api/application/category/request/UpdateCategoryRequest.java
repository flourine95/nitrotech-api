package com.nitrotech.api.application.category.request;

import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record UpdateCategoryRequest(
        @Size(max = 255, message = "Name must be at most 255 characters")
        String name,

        @Pattern(regexp = "^[a-z0-9]+(?:-[a-z0-9]+)*$", message = "Slug must be lowercase letters, numbers and hyphens")
        @Size(max = 255, message = "Slug must be at most 255 characters")
        String slug,

        String description,
        String image,
        Long parentId,
        Boolean active
) {}
