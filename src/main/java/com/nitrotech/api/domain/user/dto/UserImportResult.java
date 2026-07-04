package com.nitrotech.api.domain.user.dto;

import java.util.List;
import java.util.Map;

public record UserImportResult(
        int created,
        int failed,
        List<Integer> failedRows,
        Map<Integer, String> failedReasons
) {}
