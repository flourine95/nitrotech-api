package com.nitrotech.api.application.review.request;

import jakarta.validation.constraints.*;

import java.util.List;

public record CreateReviewRequest(
        @NotNull(message = "Product is required")
        Long productId,

        @NotNull(message = "Order is required")
        Long orderId,

        @NotNull @Min(1) @Max(5)
        Integer rating,

        @Size(max = 2000)
        String comment,

        List<String> images
) {}
