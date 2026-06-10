package com.nitrotech.api.domain.banner.dto;

import java.time.Instant;

public record CreateBannerCommand(
        String title,
        String image,
        String url,
        String position,
        boolean active,
        Instant startDate,
        Instant endDate,
        int sortOrder
) {}
