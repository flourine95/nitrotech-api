package com.nitrotech.api.domain.audit.dto;

import java.util.List;
import java.util.Map;

public record AuditLogData(
        String correlationId,
        String actorType,
        Long actorId,
        String actorEmail,
        List<String> actorRoles,
        String action,
        String resourceType,
        String resourceId,
        String outcome,
        Map<String, Object> beforeData,
        Map<String, Object> afterData,
        Map<String, Object> metadata,
        String ipAddress,
        String userAgent
) {}
