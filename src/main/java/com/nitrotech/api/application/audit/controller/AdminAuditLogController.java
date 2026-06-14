package com.nitrotech.api.application.audit.controller;

import com.nitrotech.api.domain.audit.dto.AuditLogEntryData;
import com.nitrotech.api.domain.audit.dto.AuditLogFacetsData;
import com.nitrotech.api.domain.audit.dto.AuditLogQuery;
import com.nitrotech.api.domain.audit.usecase.GetAuditLogFacetsUseCase;
import com.nitrotech.api.domain.audit.usecase.GetAuditLogsUseCase;
import com.nitrotech.api.shared.response.ApiResult;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/admin/audit-logs")
@RequiredArgsConstructor
public class AdminAuditLogController {

    private final GetAuditLogsUseCase getAuditLogsUseCase;
    private final GetAuditLogFacetsUseCase getAuditLogFacetsUseCase;

    @GetMapping
    @PreAuthorize("hasAuthority('AUDIT_LOG_READ')")
    public ResponseEntity<ApiResult<List<AuditLogEntryData>>> list(
            @RequestParam(required = false) String action,
            @RequestParam(required = false) String actor,
            @RequestParam(required = false) String resourceType,
            @RequestParam(required = false) String outcome,
            @RequestParam(required = false) String correlationId,
            @RequestParam(required = false) String resourceId,
            @RequestParam(defaultValue = "createdAt") String sortBy,
            @RequestParam(defaultValue = "desc") String sortDir,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size
    ) {
        Page<AuditLogEntryData> result = getAuditLogsUseCase.execute(
                new AuditLogQuery(action, actor, resourceType, outcome, correlationId, resourceId, sortBy, sortDir, page, size)
        );
        AuditLogFacetsData facets = getAuditLogFacetsUseCase.execute();
        return ResponseEntity.ok(ApiResult.paged(result, facets));
    }
}
