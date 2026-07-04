package com.nitrotech.api.domain.user.dto;

import java.time.Instant;

public record AdminUserFilter(
        String search,
        String status,
        String provider,
        String role,
        String activity,
        Instant createdFrom,
        Instant createdToExclusive,
        Boolean deleted
) {}
