package com.nitrotech.api.application.brand.request;

import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record UpdateBrandRequest(
        @Size(max = 255, message = "Name must be at most 255 characters")
        String name,

        @Pattern(regexp = "^[a-z0-9]+(?:-[a-z0-9]+)*$", message = "Slug must be lowercase letters, numbers and hyphens")
        @Size(max = 255, message = "Slug must be at most 255 characters")
        String slug,

        @Size(max = 500, message = "Logo URL must be at most 500 characters")
        String logo,

        String description,
        Boolean active
) {}
