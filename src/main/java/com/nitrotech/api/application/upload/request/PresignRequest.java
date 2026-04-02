package com.nitrotech.api.application.upload.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

public record PresignRequest(
        @NotBlank(message = "Filename is required")
        String filename,

        @NotBlank(message = "Content type is required")
        @Pattern(regexp = "^image/(jpeg|png|webp|gif|svg\\+xml)$",
                message = "Only image files are allowed (jpeg, png, webp, gif, svg)")
        String contentType,

        // Folder để tổ chức file: products, brands, categories, avatars, banners
        @Pattern(regexp = "^(products|brands|categories|avatars|banners)$",
                message = "Invalid folder")
        String folder
) {}
