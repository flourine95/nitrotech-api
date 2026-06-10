package com.nitrotech.api.domain.review.dto;

import java.time.Instant;
import java.util.List;

public record ReviewData(
        Long id,
        Long productId,
        Long userId,
        String userName,
        Long orderId,
        int rating,
        String comment,
        List<String> images,
        String status,
        Instant createdAt,
        Instant updatedAt
) {}
