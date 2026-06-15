package com.nitrotech.api.infrastructure.persistence.repository;

import com.nitrotech.api.domain.audit.dto.AuditLogData;
import com.nitrotech.api.domain.audit.repository.AuditLogRepository;
import com.nitrotech.api.infrastructure.persistence.mapper.AuditLogMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

@Repository
@RequiredArgsConstructor
public class AuditLogRepositoryImpl implements AuditLogRepository {

    private final AuditLogJpaRepository jpa;
    private final AuditLogMapper mapper;

    @Override
    public void save(AuditLogData data) {
        jpa.save(mapper.toEntity(data));
    }
}
