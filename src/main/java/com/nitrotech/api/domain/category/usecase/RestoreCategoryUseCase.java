package com.nitrotech.api.domain.category.usecase;

import com.nitrotech.api.domain.audit.AuditAction;
import com.nitrotech.api.domain.audit.AuditResourceType;
import com.nitrotech.api.domain.audit.dto.AuditLogCommand;
import com.nitrotech.api.domain.audit.service.AuditLogService;
import com.nitrotech.api.domain.category.exception.CategoryNotFoundException;
import com.nitrotech.api.domain.category.exception.CategorySlugConflictException;

import com.nitrotech.api.domain.category.repository.CategoryRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Map;

@Service
@RequiredArgsConstructor
public class RestoreCategoryUseCase {

    private final CategoryRepository categoryRepository;
    private final AuditLogService auditLogService;

    @Transactional
    public void execute(Long id) {
        // Tìm record đã deleted — nếu không tìm thấy thì 404
        var category = categoryRepository.findDeletedById(id)
                .orElseThrow(() -> new CategoryNotFoundException("Deleted category not found"));

        // Check slug conflict với records chưa bị xóa
        if (categoryRepository.existsNotDeletedBySlugAndIdNot(category.slug(), id)) {
            throw new CategorySlugConflictException(category.slug());
        }

        categoryRepository.restore(id);
        auditLogService.record(AuditLogCommand.success(
                AuditAction.CATEGORY_RESTORED,
                AuditResourceType.CATEGORY,
                id,
                null,
                Map.of("slug", category.slug(), "deleted", false),
                null
        ));
    }
}
