package com.nitrotech.api.domain.category.dto;

import java.util.List;
import java.util.Map;

public record ValidateDeleteResult(
        List<Long> canDelete,
        List<Long> cannotDelete,
        Map<Long, String> reasons
) {}
