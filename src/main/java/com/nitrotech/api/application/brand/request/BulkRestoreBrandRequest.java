package com.nitrotech.api.application.brand.request;

import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Size;

import java.util.List;

public record BulkRestoreBrandRequest(
        @NotEmpty(message = "IDs must not be empty")
        @Size(max = 100, message = "Cannot restore more than 100 brands at once")
        List<Long> ids
) {}
