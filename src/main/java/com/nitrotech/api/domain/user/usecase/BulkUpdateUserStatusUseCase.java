package com.nitrotech.api.domain.user.usecase;

import com.nitrotech.api.domain.auth.UserStatus;
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
public class BulkUpdateUserStatusUseCase {

    private final AdminUserRepository adminUserRepository;
    private final AuditLogService auditLogService;
    private final AuthSessionInvalidator authSessionInvalidator;

    @Transactional
    public BulkResult execute(List<Long> ids, String status, Long currentUserId) {
        Map<Long, String> failedReasons = new LinkedHashMap<>();

        List<Long> validIds = ids.stream()
                .filter(id -> {
                    if (id.equals(currentUserId)) {
                        failedReasons.put(id, "Không thể tự thay đổi trạng thái của chính mình");
                        return false;
                    }
                    if (id.equals(1L)) {
                        failedReasons.put(id, "Không thể thay đổi trạng thái tài khoản Quản trị viên tối cao (ID = 1)");
                        return false;
                    }
                    return true;
                }).toList();

        Map<Long, String> beforeStatuses = adminUserRepository.findStatusesByIds(validIds);
        List<Long> updated = validIds.isEmpty() ? List.of() : adminUserRepository.bulkUpdateStatus(validIds, status);
        Set<Long> updatedSet = Set.copyOf(updated);
        UserStatus requestedStatus = UserStatus.fromValue(status);

        if (!updated.isEmpty()) {
            if (requestedStatus != UserStatus.active) {
                authSessionInvalidator.invalidateByEmails(adminUserRepository.findEmailsByIds(updated));
            }

            auditLogService.record(AuditLogCommand.success(
                    AuditAction.USER_STATUS_UPDATED,
                    AuditResourceType.USER,
                    String.join(",", updated.stream().map(String::valueOf).toList()),
                    Map.of("statuses", beforeStatuses),
                    Map.of("status", status),
                    null
            ));
        }

        for (Long id : ids) {
            if (!failedReasons.containsKey(id) && !updatedSet.contains(id)) {
                failedReasons.put(id, "Tài khoản không tồn tại, đã bị xóa hoặc trạng thái không hợp lệ");
            }
        }

        List<Long> failed = List.copyOf(failedReasons.keySet());
        return new BulkResult(updated.size(), failed.size(), failed, failedReasons);
    }
}
