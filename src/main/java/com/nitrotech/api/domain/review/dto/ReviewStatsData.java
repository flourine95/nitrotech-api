package com.nitrotech.api.domain.review.dto;

public record ReviewStatsData(
        Long productId,
        double averageRating,
        long total,
        long fiveStars,
        long fourStars,
        long threeStars,
        long twoStars,
        long oneStar
) {}
