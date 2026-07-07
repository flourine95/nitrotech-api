package com.nitrotech.api.domain.banner.usecase;

import com.nitrotech.api.domain.audit.AuditAction;
import com.nitrotech.api.domain.audit.AuditResourceType;
import com.nitrotech.api.domain.audit.dto.AuditLogCommand;
import com.nitrotech.api.domain.audit.service.AuditLogService;
import com.nitrotech.api.domain.banner.dto.BannerData;
import com.nitrotech.api.domain.banner.dto.UpdateBannerCommand;
import com.nitrotech.api.domain.banner.exception.BannerNotFoundException;
import com.nitrotech.api.domain.banner.repository.BannerRepository;
import com.nitrotech.api.domain.shared.exception.InvalidDateRangeException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Map;

@Service
@RequiredArgsConstructor
public class UpdateBannerUseCase {

    private final BannerRepository bannerRepository;
    private final AuditLogService auditLogService;

    @Transactional
    public BannerData execute(UpdateBannerCommand command) {
        if (!bannerRepository.existsById(command.id())) {
            throw new BannerNotFoundException();
        }
        if (command.startDate() != null && command.endDate() != null
                && command.startDate().isAfter(command.endDate())) {
            throw new InvalidDateRangeException();
        }
        BannerData updated = bannerRepository.update(command);
        auditLogService.record(AuditLogCommand.success(
                AuditAction.BANNER_UPDATED,
                AuditResourceType.BANNER,
                updated.id(),
                null,
                Map.of("title", updated.title(), "position", updated.position(), "active", updated.active()),
                null
        ));
        return updated;
    }
}
