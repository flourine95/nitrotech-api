package com.nitrotech.api.application.review.request;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Size;

import java.util.List;

public record UpdateReviewRequest(
        @Min(1) @Max(5)
        Integer rating,

        @Size(max = 2000, message = "Comment must not exceed 2000 characters")
        String comment,

        List<String> images
) {}
