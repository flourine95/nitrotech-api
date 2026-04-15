package com.nitrotech.api.application.product.request;

import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

import java.util.List;
import java.util.Map;

public record UpdateProductRequest(
        Long categoryId,
        Long brandId,

        @Size(max = 255, message = "Name must be at most 255 characters")
        String name,

        @Pattern(regexp = "^[a-z0-9]+(?:-[a-z0-9]+)*$", message = "Slug must be lowercase letters, numbers and hyphens")
        @Size(max = 255, message = "Slug must be at most 255 characters")
        String slug,

        String description,
        String thumbnail,
        Map<String, Object> specs,
        Boolean active,
        List<String> images
) {}
