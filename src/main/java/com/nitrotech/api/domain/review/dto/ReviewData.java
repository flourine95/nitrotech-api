package com.nitrotech.api.domain.review.dto;

import java.time.LocalDateTime;
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
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {}
