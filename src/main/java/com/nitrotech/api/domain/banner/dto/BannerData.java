package com.nitrotech.api.domain.banner.dto;

import java.time.Instant;

public record BannerData(
        Long id,
        String title,
        String image,
        String url,
        String position,
        boolean active,
        Instant startDate,
        Instant endDate,
        int sortOrder,
        Instant createdAt,
        Instant updatedAt
) {}
