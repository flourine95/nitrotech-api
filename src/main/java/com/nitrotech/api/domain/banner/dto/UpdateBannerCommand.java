package com.nitrotech.api.domain.banner.dto;

import java.time.LocalDateTime;

public record UpdateBannerCommand(
        Long id,
        String title,
        String image,
        String url,
        String position,
        Boolean active,
        LocalDateTime startDate,
        LocalDateTime endDate,
        Integer sortOrder
) {}
