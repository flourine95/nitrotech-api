package com.nitrotech.api.domain.category.usecase;

import com.nitrotech.api.domain.audit.AuditAction;
import com.nitrotech.api.domain.audit.AuditResourceType;
import com.nitrotech.api.domain.audit.dto.AuditLogCommand;
import com.nitrotech.api.domain.audit.service.AuditLogService;
import com.nitrotech.api.domain.category.dto.CategoryData;
import com.nitrotech.api.domain.category.dto.UpdateCategoryCommand;
import com.nitrotech.api.domain.category.exception.CategoryNotFoundException;
import com.nitrotech.api.domain.category.repository.CategoryRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Map;

@Service
@RequiredArgsConstructor
public class ToggleCategoryUseCase {

    private final CategoryRepository categoryRepository;
    private final AuditLogService auditLogService;

    @Transactional
    public CategoryData execute(Long id) {
        CategoryData category = categoryRepository.findNotDeletedById(id)
                .orElseThrow(() -> new CategoryNotFoundException());

        // Toggle active status
        boolean newActiveStatus = !category.active();

        CategoryData updated = categoryRepository.update(new UpdateCategoryCommand(
                id,
                null,  // name
                null,  // slug
                null,  // description
                null,  // image
                null,  // parentId
                newActiveStatus  // active
        ));
        auditLogService.record(AuditLogCommand.success(
                AuditAction.CATEGORY_UPDATED,
                AuditResourceType.CATEGORY,
                id,
                Map.of("active", category.active()),
                Map.of("active", updated.active()),
                null
        ));
        return updated;
    }
}
