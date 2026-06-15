package com.nitrotech.api.domain.audit.usecase;

import com.nitrotech.api.domain.audit.dto.AuditAction;
import com.nitrotech.api.domain.audit.dto.AuditLogFacetsData;
import com.nitrotech.api.domain.audit.dto.AuditOutcome;
import com.nitrotech.api.domain.audit.dto.AuditResourceType;
import org.springframework.stereotype.Service;

import java.util.Arrays;

@Service
public class GetAuditLogFacetsUseCase {

    public AuditLogFacetsData execute() {
        return new AuditLogFacetsData(
                Arrays.stream(AuditAction.values()).map(Enum::name).toList(),
                Arrays.stream(AuditResourceType.values()).map(Enum::name).toList(),
                Arrays.stream(AuditOutcome.values()).map(Enum::name).toList()
        );
    }
}
