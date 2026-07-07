package com.nitrotech.api.domain.promotion.usecase;

import com.nitrotech.api.domain.audit.AuditAction;
import com.nitrotech.api.domain.audit.AuditResourceType;
import com.nitrotech.api.domain.audit.dto.AuditLogCommand;
import com.nitrotech.api.domain.audit.service.AuditLogService;
import com.nitrotech.api.domain.promotion.dto.CreatePromotionCommand;
import com.nitrotech.api.domain.promotion.dto.PromotionData;
import com.nitrotech.api.domain.promotion.exception.PromotionCodeExistsException;
import com.nitrotech.api.domain.promotion.exception.PromotionNotFoundException;
import com.nitrotech.api.domain.promotion.repository.PromotionRepository;
import com.nitrotech.api.domain.shared.exception.InvalidDateRangeException;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Map;

@Service
@RequiredArgsConstructor
public class ManagePromotionUseCase {

    private final PromotionRepository promotionRepository;
    private final AuditLogService auditLogService;

    @Transactional
    public PromotionData create(CreatePromotionCommand command) {
        if (command.code() != null && promotionRepository.existsByCode(command.code())) {
            throw new PromotionCodeExistsException();
        }
        if (command.startAt().isAfter(command.endAt())) {
            throw new InvalidDateRangeException();
        }
        PromotionData promotion = promotionRepository.create(command);
        auditLogService.record(AuditLogCommand.success(
                AuditAction.PROMOTION_CREATED,
                AuditResourceType.PROMOTION,
                promotion.id(),
                null,
                Map.of("code", promotion.code(), "status", promotion.status(), "type", promotion.type()),
                null
        ));
        return promotion;
    }

    @Transactional
    public PromotionData update(Long id, CreatePromotionCommand command) {
        if (!promotionRepository.existsById(id)) {
            throw new PromotionNotFoundException();
        }
        if (command.code() != null && promotionRepository.existsByCodeAndIdNot(command.code(), id)) {
            throw new PromotionCodeExistsException();
        }
        PromotionData updated = promotionRepository.update(id, command);
        auditLogService.record(AuditLogCommand.success(
                AuditAction.PROMOTION_UPDATED,
                AuditResourceType.PROMOTION,
                id,
                null,
                Map.of("code", updated.code(), "status", updated.status(), "type", updated.type()),
                null
        ));
        return updated;
    }

    @Transactional
    public PromotionData updateStatus(Long id, String status) {
        if (!promotionRepository.existsById(id)) {
            throw new PromotionNotFoundException();
        }
        PromotionData updated = promotionRepository.updateStatus(id, status);
        auditLogService.record(AuditLogCommand.success(
                AuditAction.PROMOTION_STATUS_UPDATED,
                AuditResourceType.PROMOTION,
                id,
                null,
                Map.of("status", updated.status()),
                null
        ));
        return updated;
    }

    public Page<PromotionData> findAll(String status, int page, int size) {
        return promotionRepository.findAll(status, PageRequest.of(page, size));
    }

    public PromotionData findById(Long id) {
        return promotionRepository.findById(id)
                .orElseThrow(() -> new PromotionNotFoundException());
    }

    @Transactional
    public void delete(Long id) {
        if (!promotionRepository.existsById(id)) {
            throw new PromotionNotFoundException();
        }
        promotionRepository.delete(id);
        auditLogService.record(AuditLogCommand.success(
                AuditAction.PROMOTION_DELETED,
                AuditResourceType.PROMOTION,
                id,
                null,
                Map.of("deleted", true),
                null
        ));
    }
}
