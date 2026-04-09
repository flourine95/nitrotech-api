package com.nitrotech.api.application.category.request;

import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;

import java.util.List;

public record MoveCategoryRequest(
        @NotNull(message = "movedId is required")
        Long movedId,

        Long fromParentId,  // null = từ root

        Long toParentId,    // null = move lên root

        @NotEmpty(message = "targetOrderedIds must not be empty")
        List<Long> targetOrderedIds,

        List<Long> sourceOrderedIds  // optional nếu fromParentId = toParentId
) {}
