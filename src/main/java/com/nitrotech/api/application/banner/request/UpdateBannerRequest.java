package com.nitrotech.api.application.banner.request;

import jakarta.validation.constraints.Size;

import java.time.LocalDateTime;

public record UpdateBannerRequest(
        @Size(max = 255)
        String title,

        @Size(max = 500)
        String image,

        @Size(max = 500)
        String url,

        String position,
        Boolean active,
        LocalDateTime startDate,
        LocalDateTime endDate,
        Integer sortOrder
) {}
