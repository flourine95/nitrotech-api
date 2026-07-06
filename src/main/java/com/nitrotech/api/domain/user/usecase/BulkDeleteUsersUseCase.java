package com.nitrotech.api.domain.user.usecase;

import com.nitrotech.api.domain.audit.AuditAction;
import com.nitrotech.api.domain.audit.AuditResourceType;
import com.nitrotech.api.domain.audit.dto.AuditLogCommand;
import com.nitrotech.api.domain.audit.service.AuditLogService;
import com.nitrotech.api.domain.auth.service.AuthSessionInvalidator;
import com.nitrotech.api.domain.user.dto.BulkResult;
import com.nitrotech.api.domain.user.repository.AdminUserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

@Service
@RequiredArgsConstructor
public class BulkDeleteUsersUseCase {

    private final AdminUserRepository adminUserRepository;
    private final AuditLogService auditLogService;
    private final AuthSessionInvalidator authSessionInvalidator;

    @Transactional
    public BulkResult execute(List<Long> ids, Long currentUserId) {
        Map<Long, String> failedReasons = new LinkedHashMap<>();
        
        List<Long> validIds = ids.stream()
                .filter(id -> {
                    if (id.equals(currentUserId)) {
                        failedReasons.put(id, "Không thể tự xóa tài khoản của chính mình");
                        return false;
                    }
                    if (id.equals(1L)) {
                        failedReasons.put(id, "Không thể xóa tài khoản Quản trị viên tối cao (ID = 1)");
                        return false;
                    }
                    return true;
                }).toList();

        Map<Long, String> beforeStatuses = adminUserRepository.findStatusesByIds(validIds);
        List<Long> deleted = validIds.isEmpty() ? List.of() : adminUserRepository.bulkSoftDelete(validIds);
        Set<Long> deletedSet = Set.copyOf(deleted);

        if (!deleted.isEmpty()) {
            authSessionInvalidator.invalidateByEmails(adminUserRepository.findEmailsByIds(deleted));

            auditLogService.record(AuditLogCommand.success(
                    AuditAction.USER_DELETED,
                    AuditResourceType.USER,
                    String.join(",", deleted.stream().map(String::valueOf).toList()),
                    Map.of("statuses", beforeStatuses, "deleted", false),
                    Map.of("deleted", true),
                    null
            ));
        }

        for (Long id : ids) {
            if (!failedReasons.containsKey(id) && !deletedSet.contains(id)) {
                failedReasons.put(id, "Tài khoản không tồn tại hoặc đã bị xóa trước đó");
            }
        }

        List<Long> failed = List.copyOf(failedReasons.keySet());
        return new BulkResult(deleted.size(), failed.size(), failed, failedReasons);
    }
}
