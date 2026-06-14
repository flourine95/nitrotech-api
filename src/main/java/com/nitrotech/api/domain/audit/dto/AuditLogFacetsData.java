package com.nitrotech.api.domain.audit.dto;

import java.util.List;

public record AuditLogFacetsData(
        List<String> actions,
        List<String> resourceTypes,
        List<String> outcomes
) {}
