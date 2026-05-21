package com.nitrotech.api.application.banner.request;

import jakarta.validation.constraints.Size;

import java.time.LocalDateTime;

public record UpdateBannerRequest(
        @Size(max = 255, message = "Title must be at most 255 characters")
        String title,

        @Size(max = 500, message = "Image must be at most 500 characters")
        String image,

        @Size(max = 500, message = "URL must be at most 500 characters")
        String url,

        String position,
        Boolean active,
        LocalDateTime startDate,
        LocalDateTime endDate,
        Integer sortOrder
) {}
