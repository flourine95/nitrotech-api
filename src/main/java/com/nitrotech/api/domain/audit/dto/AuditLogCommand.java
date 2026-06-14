package com.nitrotech.api.domain.audit.dto;

import java.util.Map;

public record AuditLogCommand(
        String actorType,
        Long actorId,
        String actorEmail,
        String action,
        String resourceType,
        String resourceId,
        String outcome,
        Map<String, Object> beforeData,
        Map<String, Object> afterData,
        Map<String, Object> metadata
) {
    public static AuditLogCommand success(
            String action,
            String resourceType,
            Object resourceId,
            Map<String, Object> beforeData,
            Map<String, Object> afterData,
            Map<String, Object> metadata
    ) {
        return new AuditLogCommand(
                null, null, null,
                action,
                resourceType,
                resourceId == null ? null : String.valueOf(resourceId),
                "SUCCESS",
                beforeData,
                afterData,
                metadata
        );
    }
}
