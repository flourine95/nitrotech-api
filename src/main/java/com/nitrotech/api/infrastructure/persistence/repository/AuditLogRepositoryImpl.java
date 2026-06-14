package com.nitrotech.api.infrastructure.persistence.repository;

import com.nitrotech.api.domain.audit.dto.AuditLogData;
import com.nitrotech.api.domain.audit.repository.AuditLogRepository;
import com.nitrotech.api.infrastructure.persistence.entity.AuditLogEntity;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

@Repository
@RequiredArgsConstructor
public class AuditLogRepositoryImpl implements AuditLogRepository {

    private final AuditLogJpaRepository jpa;

    @Override
    public void save(AuditLogData data) {
        AuditLogEntity entity = new AuditLogEntity();
        entity.setCorrelationId(data.correlationId());
        entity.setActorType(data.actorType());
        entity.setActorId(data.actorId());
        entity.setActorEmail(data.actorEmail());
        entity.setActorRoles(data.actorRoles());
        entity.setAction(data.action());
        entity.setResourceType(data.resourceType());
        entity.setResourceId(data.resourceId());
        entity.setOutcome(data.outcome());
        entity.setBeforeData(data.beforeData());
        entity.setAfterData(data.afterData());
        entity.setMetadata(data.metadata());
        entity.setIpAddress(data.ipAddress());
        entity.setUserAgent(data.userAgent());
        jpa.save(entity);
    }
}
