package com.nitrotech.api.domain.category.usecase;

import com.nitrotech.api.domain.audit.AuditAction;
import com.nitrotech.api.domain.audit.AuditResourceType;
import com.nitrotech.api.domain.audit.dto.AuditLogCommand;
import com.nitrotech.api.domain.audit.service.AuditLogService;
import com.nitrotech.api.domain.category.exception.CategoryHasChildrenException;
import com.nitrotech.api.domain.category.exception.CategoryHasProductsException;
import com.nitrotech.api.domain.category.exception.CategoryNotFoundException;

import com.nitrotech.api.domain.category.repository.CategoryRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Map;

@Service
@RequiredArgsConstructor
public class HardDeleteCategoryUseCase {

    private final CategoryRepository categoryRepository;
    private final ProductCategoryChecker productCategoryChecker;
    private final AuditLogService auditLogService;

    @Transactional
    public void execute(Long id) {
        // Chỉ cho hard delete record đã soft deleted
        categoryRepository.findDeletedById(id)
                .orElseThrow(CategoryNotFoundException::deletedForHardDelete);

        // Block nếu còn children (kể cả deleted)
        if (categoryRepository.hasAnyChildren(id)) {
            throw new CategoryHasChildrenException("Cannot permanently delete category with subcategories.");
        }

        // Block nếu có product đang dùng category này
        if (productCategoryChecker.hasProducts(id)) {
            throw new CategoryHasProductsException();
        }

        categoryRepository.hardDelete(id);
        auditLogService.record(AuditLogCommand.success(
                AuditAction.CATEGORY_HARD_DELETED,
                AuditResourceType.CATEGORY,
                id,
                null,
                Map.of("hardDeleted", true),
                null
        ));
    }
}
