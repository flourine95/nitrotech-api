package com.nitrotech.api.application.banner.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

import java.time.LocalDateTime;

public record CreateBannerRequest(
        @NotBlank(message = "Title is required")
        @Size(max = 255)
        String title,

        @NotBlank(message = "Image is required")
        @Size(max = 500)
        String image,

        @Size(max = 500)
        String url,

        @NotBlank(message = "Position is required")
        String position,

        boolean active,
        LocalDateTime startDate,
        LocalDateTime endDate,
        int sortOrder
) {}
