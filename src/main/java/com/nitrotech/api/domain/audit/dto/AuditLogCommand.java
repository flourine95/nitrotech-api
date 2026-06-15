package com.nitrotech.api.domain.audit.dto;

import java.util.Map;

public record AuditLogCommand(
        AuditActorType actorType,
        Long actorId,
        String actorEmail,
        AuditAction action,
        AuditResourceType resourceType,
        String resourceId,
        AuditOutcome outcome,
        Map<String, Object> beforeData,
        Map<String, Object> afterData,
        Map<String, Object> metadata
) {
    public static AuditLogCommand success(
            AuditAction action,
            AuditResourceType resourceType,
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
                AuditOutcome.SUCCESS,
                beforeData,
                afterData,
                metadata
        );
    }
}
