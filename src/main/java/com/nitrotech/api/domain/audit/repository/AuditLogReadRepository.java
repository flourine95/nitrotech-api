package com.nitrotech.api.domain.audit.repository;

import com.nitrotech.api.domain.audit.dto.AuditLogEntryData;
import com.nitrotech.api.domain.audit.dto.AuditLogQuery;
import org.springframework.data.domain.Page;

public interface AuditLogReadRepository {
    Page<AuditLogEntryData> findAll(AuditLogQuery query);
}
