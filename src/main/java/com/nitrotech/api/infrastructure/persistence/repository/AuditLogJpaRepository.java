package com.nitrotech.api.infrastructure.persistence.repository;

import com.nitrotech.api.infrastructure.persistence.entity.AuditLogEntity;
import org.springframework.data.repository.Repository;

public interface AuditLogJpaRepository extends Repository<AuditLogEntity, Long> {
    AuditLogEntity save(AuditLogEntity auditLog);
}
