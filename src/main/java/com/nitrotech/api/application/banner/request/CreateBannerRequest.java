package com.nitrotech.api.application.banner.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

import java.time.Instant;

public record CreateBannerRequest(
        @NotBlank(message = "Title is required")
        @Size(max = 255, message = "Title must be at most 255 characters")
        String title,

        @NotBlank(message = "Image is required")
        @Size(max = 500, message = "Image must be at most 500 characters")
        String image,

        @Size(max = 500, message = "URL must be at most 500 characters")
        String url,

        @NotBlank(message = "Position is required")
        String position,

        boolean active,
        Instant startDate,
        Instant endDate,
        int sortOrder
) {}
