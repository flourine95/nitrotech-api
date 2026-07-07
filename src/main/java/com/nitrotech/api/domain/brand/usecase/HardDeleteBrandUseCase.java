package com.nitrotech.api.domain.brand.usecase;

import com.nitrotech.api.domain.audit.AuditAction;
import com.nitrotech.api.domain.audit.AuditResourceType;
import com.nitrotech.api.domain.audit.dto.AuditLogCommand;
import com.nitrotech.api.domain.audit.service.AuditLogService;
import com.nitrotech.api.domain.brand.exception.BrandHasProductsException;
import com.nitrotech.api.domain.brand.exception.BrandNotFoundException;
import com.nitrotech.api.domain.brand.repository.BrandRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Map;

@Service
@RequiredArgsConstructor
public class HardDeleteBrandUseCase {

    private final BrandRepository brandRepository;
    private final ProductBrandChecker productBrandChecker;
    private final AuditLogService auditLogService;

    @Transactional
    public void execute(Long id) {
        brandRepository.findDeletedById(id)
                .orElseThrow(BrandNotFoundException::deletedForHardDelete);

        if (productBrandChecker.hasProducts(id)) {
            throw new BrandHasProductsException();
        }

        brandRepository.hardDelete(id);
        auditLogService.record(AuditLogCommand.success(
                AuditAction.BRAND_HARD_DELETED,
                AuditResourceType.BRAND,
                id,
                null,
                Map.of("hardDeleted", true),
                null
        ));
    }
}
