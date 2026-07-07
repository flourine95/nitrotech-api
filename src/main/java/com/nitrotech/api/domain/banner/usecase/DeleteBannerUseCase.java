package com.nitrotech.api.domain.banner.usecase;

import com.nitrotech.api.domain.audit.AuditAction;
import com.nitrotech.api.domain.audit.AuditResourceType;
import com.nitrotech.api.domain.audit.dto.AuditLogCommand;
import com.nitrotech.api.domain.audit.service.AuditLogService;
import com.nitrotech.api.domain.banner.exception.BannerNotFoundException;
import com.nitrotech.api.domain.banner.repository.BannerRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Map;

@Service
@RequiredArgsConstructor
public class DeleteBannerUseCase {

    private final BannerRepository bannerRepository;
    private final AuditLogService auditLogService;

    @Transactional
    public void execute(Long id) {
        if (!bannerRepository.existsById(id)) {
            throw new BannerNotFoundException();
        }
        bannerRepository.delete(id);
        auditLogService.record(AuditLogCommand.success(
                AuditAction.BANNER_DELETED,
                AuditResourceType.BANNER,
                id,
                null,
                Map.of("deleted", true),
                null
        ));
    }
}
