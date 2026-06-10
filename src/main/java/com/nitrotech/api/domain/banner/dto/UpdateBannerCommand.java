package com.nitrotech.api.domain.banner.dto;

import java.time.Instant;

public record UpdateBannerCommand(
        Long id,
        String title,
        String image,
        String url,
        String position,
        Boolean active,
        Instant startDate,
        Instant endDate,
        Integer sortOrder
) {}
