package com.nitrotech.api.application.upload.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

public record PresignRequest(
        @NotBlank(message = "Folder is required")
        @Pattern(regexp = "^(products|brands|categories|avatars|banners)$",
                message = "Invalid folder. Allowed: products, brands, categories, avatars, banners")
        String folder
) {}
