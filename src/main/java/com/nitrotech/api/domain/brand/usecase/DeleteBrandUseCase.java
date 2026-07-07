package com.nitrotech.api.domain.brand.usecase;

import com.nitrotech.api.domain.audit.AuditAction;
import com.nitrotech.api.domain.audit.AuditResourceType;
import com.nitrotech.api.domain.audit.dto.AuditLogCommand;
import com.nitrotech.api.domain.audit.service.AuditLogService;
import com.nitrotech.api.domain.brand.exception.BrandNotFoundException;
import com.nitrotech.api.domain.brand.repository.BrandRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Map;

@Service
@RequiredArgsConstructor
public class DeleteBrandUseCase {

    private final BrandRepository brandRepository;
    private final AuditLogService auditLogService;

    @Transactional
    public void execute(Long id) {
        if (!brandRepository.existsById(id)) {
            throw new BrandNotFoundException();
        }
        brandRepository.softDelete(id);
        auditLogService.record(AuditLogCommand.success(
                AuditAction.BRAND_DELETED,
                AuditResourceType.BRAND,
                id,
                null,
                Map.of("deleted", true),
                null
        ));
    }
}
