package com.nitrotech.api.domain.category.usecase;

import com.nitrotech.api.domain.audit.AuditAction;
import com.nitrotech.api.domain.audit.AuditResourceType;
import com.nitrotech.api.domain.audit.dto.AuditLogCommand;
import com.nitrotech.api.domain.audit.service.AuditLogService;
import com.nitrotech.api.domain.category.dto.CategoryData;
import com.nitrotech.api.domain.category.dto.CreateCategoryCommand;
import com.nitrotech.api.domain.category.exception.CategoryNotFoundException;
import com.nitrotech.api.domain.category.exception.CategorySlugExistsException;
import com.nitrotech.api.domain.category.repository.CategoryRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Map;

@Service
@RequiredArgsConstructor
public class CreateCategoryUseCase {

    private final CategoryRepository categoryRepository;
    private final AuditLogService auditLogService;

    @Transactional
    public CategoryData execute(CreateCategoryCommand command) {
        if (categoryRepository.existsNotDeletedBySlug(command.slug())) {
            throw new CategorySlugExistsException(command.slug());
        }
        if (command.parentId() != null && !categoryRepository.existsById(command.parentId())) {
            throw CategoryNotFoundException.parentWithId(command.parentId());
        }
        CategoryData category = categoryRepository.create(command);
        auditLogService.record(AuditLogCommand.success(
                AuditAction.CATEGORY_CREATED,
                AuditResourceType.CATEGORY,
                category.id(),
                null,
                Map.of("name", category.name(), "slug", category.slug(), "active", category.active()),
                null
        ));
        return category;
    }
}
