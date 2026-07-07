package com.nitrotech.api.domain.brand.usecase;

import com.nitrotech.api.domain.audit.AuditAction;
import com.nitrotech.api.domain.audit.AuditResourceType;
import com.nitrotech.api.domain.audit.dto.AuditLogCommand;
import com.nitrotech.api.domain.audit.service.AuditLogService;
import com.nitrotech.api.domain.brand.dto.BrandData;
import com.nitrotech.api.domain.brand.dto.UpdateBrandCommand;
import com.nitrotech.api.domain.brand.exception.BrandNotFoundException;
import com.nitrotech.api.domain.brand.exception.BrandSlugExistsException;
import com.nitrotech.api.domain.brand.repository.BrandRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Map;

@Service
@RequiredArgsConstructor
public class UpdateBrandUseCase {

    private final BrandRepository brandRepository;
    private final AuditLogService auditLogService;

    @Transactional
    public BrandData execute(UpdateBrandCommand command) {
        if (!brandRepository.existsById(command.id())) {
            throw new BrandNotFoundException();
        }
        if (command.slug() != null && brandRepository.existsNotDeletedBySlugAndIdNot(command.slug(), command.id())) {
            throw new BrandSlugExistsException();
        }
        BrandData updated = brandRepository.update(command);
        auditLogService.record(AuditLogCommand.success(
                AuditAction.BRAND_UPDATED,
                AuditResourceType.BRAND,
                updated.id(),
                null,
                Map.of("name", updated.name(), "slug", updated.slug(), "active", updated.active()),
                null
        ));
        return updated;
    }
}
