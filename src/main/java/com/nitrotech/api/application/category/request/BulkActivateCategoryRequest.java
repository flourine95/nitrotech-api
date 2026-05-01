package com.nitrotech.api.application.category.request;

import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Size;

import java.util.List;

public record BulkActivateCategoryRequest(
        @NotEmpty(message = "ids must not be empty")
        @Size(max = 100, message = "Cannot activate more than 100 categories at once")
        List<Long> ids
) {}
