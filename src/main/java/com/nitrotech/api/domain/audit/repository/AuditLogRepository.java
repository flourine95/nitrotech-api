package com.nitrotech.api.domain.audit.repository;

import com.nitrotech.api.domain.audit.dto.AuditLogData;

public interface AuditLogRepository {
    void save(AuditLogData auditLog);
}
