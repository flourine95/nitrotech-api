package com.nitrotech.api.domain.brand.usecase;

import com.nitrotech.api.domain.audit.AuditAction;
import com.nitrotech.api.domain.audit.AuditResourceType;
import com.nitrotech.api.domain.audit.dto.AuditLogCommand;
import com.nitrotech.api.domain.audit.service.AuditLogService;
import com.nitrotech.api.domain.brand.exception.BrandNotFoundException;
import com.nitrotech.api.domain.brand.exception.BrandSlugConflictException;

import com.nitrotech.api.domain.brand.repository.BrandRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Map;

@Service
@RequiredArgsConstructor
public class RestoreBrandUseCase {

    private final BrandRepository brandRepository;
    private final AuditLogService auditLogService;

    @Transactional
    public void execute(Long id) {
        var brand = brandRepository.findDeletedById(id)
                .orElseThrow(() -> BrandNotFoundException.deleted());

        if (brandRepository.existsNotDeletedBySlugAndIdNot(brand.slug(), id)) {
            throw new BrandSlugConflictException(brand.slug());
        }

        brandRepository.restore(id);
        auditLogService.record(AuditLogCommand.success(
                AuditAction.BRAND_RESTORED,
                AuditResourceType.BRAND,
                id,
                null,
                Map.of("slug", brand.slug(), "deleted", false),
                null
        ));
    }
}
