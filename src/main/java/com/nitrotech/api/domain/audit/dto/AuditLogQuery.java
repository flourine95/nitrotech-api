package com.nitrotech.api.domain.audit.dto;

public record AuditLogQuery(
        String action,
        String actor,
        String resourceType,
        String outcome,
        String correlationId,
        String resourceId,
        String sortBy,
        String sortDir,
        int page,
        int size
) {}
