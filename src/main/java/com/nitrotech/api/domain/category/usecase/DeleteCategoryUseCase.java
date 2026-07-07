package com.nitrotech.api.domain.category.usecase;

import com.nitrotech.api.domain.audit.AuditAction;
import com.nitrotech.api.domain.audit.AuditResourceType;
import com.nitrotech.api.domain.audit.dto.AuditLogCommand;
import com.nitrotech.api.domain.audit.service.AuditLogService;
import com.nitrotech.api.domain.category.exception.CategoryHasChildrenException;
import com.nitrotech.api.domain.category.exception.CategoryNotFoundException;
import com.nitrotech.api.domain.category.repository.CategoryRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Map;

@Service
@RequiredArgsConstructor
public class DeleteCategoryUseCase {

    private final CategoryRepository categoryRepository;
    private final AuditLogService auditLogService;

    @Transactional
    public void execute(Long id) {
        if (!categoryRepository.existsById(id)) {
            throw CategoryNotFoundException.withId(id);
        }
        if (categoryRepository.hasNotDeletedChildren(id)) {
            throw new CategoryHasChildrenException("Cannot delete category with subcategories. Delete or move them first.");
        }
        categoryRepository.softDelete(id);
        auditLogService.record(AuditLogCommand.success(
                AuditAction.CATEGORY_DELETED,
                AuditResourceType.CATEGORY,
                id,
                null,
                Map.of("deleted", true),
                null
        ));
    }
}
