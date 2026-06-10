package com.nitrotech.api.application.product.request;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

import java.time.Instant;
import java.util.List;
import java.util.Map;

public record CreateProductRequest(
        @NotNull(message = "Category is required")
        Long categoryId,

        Long brandId,

        @NotBlank(message = "Name is required")
        @Size(max = 255, message = "Name must be at most 255 characters")
        String name,

        @NotBlank(message = "Slug is required")
        @Pattern(regexp = "^[a-z0-9]+(?:-[a-z0-9]+)*$", message = "Slug must be lowercase letters, numbers and hyphens")
        @Size(max = 255, message = "Slug must be at most 255 characters")
        String slug,

        String description,

        @Size(max = 500, message = "Short description must be at most 500 characters")
        String shortDescription,

        String thumbnail,
        Map<String, Object> specs,
        boolean active,
        List<String> images,

        @Valid
        List<CreateVariantRequest> variants,

        @Size(max = 50, message = "Manual badge must be at most 50 characters")
        String manualBadge,

        Instant manualBadgeExpiresAt
) {
}
