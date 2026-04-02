package com.nitrotech.api.domain.review.dto;

import java.util.List;

public record CreateReviewCommand(
        Long userId,
        Long productId,
        Long orderId,
        int rating,
        String comment,
        List<String> images
) {}
