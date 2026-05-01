package com.nitrotech.api.domain.category.dto;

import java.util.List;
import java.util.Map;

public record BulkResult(
        int succeeded,
        int failed,
        List<Long> failedIds,
        Map<Long, String> failedReasons
) {}
