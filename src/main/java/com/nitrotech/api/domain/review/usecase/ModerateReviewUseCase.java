package com.nitrotech.api.domain.review.usecase;

import com.nitrotech.api.domain.audit.AuditAction;
import com.nitrotech.api.domain.audit.AuditResourceType;
import com.nitrotech.api.domain.audit.dto.AuditLogCommand;
import com.nitrotech.api.domain.audit.service.AuditLogService;
import com.nitrotech.api.domain.review.ReviewStatus;
import com.nitrotech.api.domain.review.dto.ReviewData;
import com.nitrotech.api.domain.review.exception.ReviewNotFoundException;
import com.nitrotech.api.domain.review.repository.ReviewRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Map;

@Service
@RequiredArgsConstructor
public class ModerateReviewUseCase {

    private final ReviewRepository reviewRepository;
    private final AuditLogService auditLogService;

    @Transactional
    public ReviewData approve(Long id) {
        reviewRepository.findById(id)
                .orElseThrow(() -> new ReviewNotFoundException());
        ReviewData review = reviewRepository.updateStatus(id, ReviewStatus.APPROVED);
        auditLogService.record(AuditLogCommand.success(
                AuditAction.REVIEW_APPROVED,
                AuditResourceType.REVIEW,
                id,
                null,
                Map.of("status", review.status()),
                null
        ));
        return review;
    }

    @Transactional
    public ReviewData reject(Long id) {
        reviewRepository.findById(id)
                .orElseThrow(() -> new ReviewNotFoundException());
        ReviewData review = reviewRepository.updateStatus(id, ReviewStatus.REJECTED);
        auditLogService.record(AuditLogCommand.success(
                AuditAction.REVIEW_REJECTED,
                AuditResourceType.REVIEW,
                id,
                null,
                Map.of("status", review.status()),
                null
        ));
        return review;
    }
}
