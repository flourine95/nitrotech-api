package com.nitrotech.api.domain.banner.dto;

import java.time.LocalDateTime;

public record BannerData(
        Long id,
        String title,
        String image,
        String url,
        String position,
        boolean active,
        LocalDateTime startDate,
        LocalDateTime endDate,
        int sortOrder,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {}
