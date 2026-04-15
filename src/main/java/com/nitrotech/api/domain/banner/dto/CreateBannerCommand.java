package com.nitrotech.api.domain.banner.dto;

import java.time.LocalDateTime;

public record CreateBannerCommand(
        String title,
        String image,
        String url,
        String position,
        boolean active,
        LocalDateTime startDate,
        LocalDateTime endDate,
        int sortOrder
) {}
