package com.nitrotech.api.domain.brand.usecase;

import com.nitrotech.api.domain.audit.AuditAction;
import com.nitrotech.api.domain.audit.AuditResourceType;
import com.nitrotech.api.domain.audit.dto.AuditLogCommand;
import com.nitrotech.api.domain.audit.service.AuditLogService;
import com.nitrotech.api.domain.brand.dto.BrandData;
import com.nitrotech.api.domain.brand.dto.CreateBrandCommand;
import com.nitrotech.api.domain.brand.exception.BrandSlugExistsException;
import com.nitrotech.api.domain.brand.repository.BrandRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Map;

@Service
@RequiredArgsConstructor
public class CreateBrandUseCase {

    private final BrandRepository brandRepository;
    private final AuditLogService auditLogService;

    @Transactional
    public BrandData execute(CreateBrandCommand command) {
        if (brandRepository.existsNotDeletedBySlug(command.slug())) {
            throw new BrandSlugExistsException();
        }
        BrandData brand = brandRepository.create(command);
        auditLogService.record(AuditLogCommand.success(
                AuditAction.BRAND_CREATED,
                AuditResourceType.BRAND,
                brand.id(),
                null,
                Map.of("name", brand.name(), "slug", brand.slug(), "active", brand.active()),
                null
        ));
        return brand;
    }
}
