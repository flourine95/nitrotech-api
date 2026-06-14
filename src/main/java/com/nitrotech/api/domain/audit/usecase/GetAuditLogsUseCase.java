package com.nitrotech.api.domain.audit.usecase;

import com.nitrotech.api.domain.audit.dto.AuditLogEntryData;
import com.nitrotech.api.domain.audit.dto.AuditLogQuery;
import com.nitrotech.api.domain.audit.repository.AuditLogReadRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class GetAuditLogsUseCase {

    private final AuditLogReadRepository auditLogReadRepository;

    public Page<AuditLogEntryData> execute(AuditLogQuery query) {
        return auditLogReadRepository.findAll(query);
    }
}
