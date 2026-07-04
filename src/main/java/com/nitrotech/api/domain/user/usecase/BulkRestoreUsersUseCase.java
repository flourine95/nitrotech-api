package com.nitrotech.api.domain.user.usecase;

import com.nitrotech.api.domain.audit.AuditAction;
import com.nitrotech.api.domain.audit.AuditResourceType;
import com.nitrotech.api.domain.audit.dto.AuditLogCommand;
import com.nitrotech.api.domain.audit.service.AuditLogService;
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
public class BulkRestoreUsersUseCase {

    private final AdminUserRepository adminUserRepository;
    private final AuditLogService auditLogService;

    @Transactional
    public BulkResult execute(List<Long> ids) {
        Map<Long, String> failedReasons = new LinkedHashMap<>();

        List<Long> restored = ids.isEmpty() ? List.of() : adminUserRepository.bulkRestore(ids);
        Set<Long> restoredSet = Set.copyOf(restored);

        if (!restored.isEmpty()) {
            auditLogService.record(AuditLogCommand.success(
                    AuditAction.USER_RESTORED,
                    AuditResourceType.USER,
                    String.join(",", restored.stream().map(String::valueOf).toList()),
                    Map.of("ids", restored),
                    null,
                    null
            ));
        }

        for (Long id : ids) {
            if (!restoredSet.contains(id)) {
                failedReasons.put(id, "Tài khoản không tồn tại hoặc chưa bị xóa");
            }
        }

        List<Long> failed = List.copyOf(failedReasons.keySet());
        return new BulkResult(restored.size(), failed.size(), failed, failedReasons);
    }
}
