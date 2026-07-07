package com.nitrotech.api.domain.category.usecase;

import com.nitrotech.api.domain.audit.AuditAction;
import com.nitrotech.api.domain.audit.AuditResourceType;
import com.nitrotech.api.domain.audit.dto.AuditLogCommand;
import com.nitrotech.api.domain.audit.service.AuditLogService;
import com.nitrotech.api.domain.category.dto.CategoryData;
import com.nitrotech.api.domain.category.dto.UpdateCategoryCommand;
import com.nitrotech.api.domain.category.exception.CategoryCircularReferenceException;
import com.nitrotech.api.domain.category.exception.CategoryNotFoundException;
import com.nitrotech.api.domain.category.exception.CategorySlugExistsException;
import com.nitrotech.api.domain.category.repository.CategoryRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Map;

@Service
@RequiredArgsConstructor
public class UpdateCategoryUseCase {

    private final CategoryRepository categoryRepository;
    private final AuditLogService auditLogService;

    @Transactional
    public CategoryData execute(UpdateCategoryCommand command) {
        if (!categoryRepository.existsById(command.id())) {
            throw CategoryNotFoundException.withId(command.id());
        }
        if (command.slug() != null && categoryRepository.existsNotDeletedBySlugAndIdNot(command.slug(), command.id())) {
            throw new CategorySlugExistsException(command.slug());
        }
        if (command.parentId() != null) {
            if (!categoryRepository.existsById(command.parentId())) {
                throw CategoryNotFoundException.parentWithId(command.parentId());
            }
            if (command.parentId().equals(command.id())) {
                throw new CategoryCircularReferenceException("Category cannot be its own parent");
            }
            if (categoryRepository.isDescendantOf(command.parentId(), command.id())) {
                throw new CategoryCircularReferenceException("Cannot set parent: circular reference detected");
            }
        }
        CategoryData updated = categoryRepository.update(command);
        auditLogService.record(AuditLogCommand.success(
                AuditAction.CATEGORY_UPDATED,
                AuditResourceType.CATEGORY,
                updated.id(),
                null,
                Map.of("name", updated.name(), "slug", updated.slug(), "active", updated.active()),
                null
        ));
        return updated;
    }
}
