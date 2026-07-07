package com.nitrotech.api.domain.banner.usecase;

import com.nitrotech.api.domain.audit.AuditAction;
import com.nitrotech.api.domain.audit.AuditResourceType;
import com.nitrotech.api.domain.audit.dto.AuditLogCommand;
import com.nitrotech.api.domain.audit.service.AuditLogService;
import com.nitrotech.api.domain.banner.dto.BannerData;
import com.nitrotech.api.domain.banner.dto.CreateBannerCommand;
import com.nitrotech.api.domain.banner.repository.BannerRepository;
import com.nitrotech.api.domain.shared.exception.InvalidDateRangeException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Map;

@Service
@RequiredArgsConstructor
public class CreateBannerUseCase {

    private final BannerRepository bannerRepository;
    private final AuditLogService auditLogService;

    @Transactional
    public BannerData execute(CreateBannerCommand command) {
        if (command.startDate() != null && command.endDate() != null
                && command.startDate().isAfter(command.endDate())) {
            throw new InvalidDateRangeException();
        }
        BannerData banner = bannerRepository.create(command);
        auditLogService.record(AuditLogCommand.success(
                AuditAction.BANNER_CREATED,
                AuditResourceType.BANNER,
                banner.id(),
                null,
                Map.of("title", banner.title(), "position", banner.position(), "active", banner.active()),
                null
        ));
        return banner;
    }
}
