package com.nitrotech.api.domain.brand.dto;

import java.util.List;

public record BulkResult(
        int succeeded,
        int failed,
        List<Long> failedIds
) {}
