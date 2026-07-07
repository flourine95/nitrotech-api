package com.nitrotech.api.domain.user.usecase;

import com.nitrotech.api.domain.auth.UserStatus;
import com.nitrotech.api.domain.auth.exception.EmailAlreadyExistsException;
import com.nitrotech.api.domain.auth.exception.UserNotFoundException;
import com.nitrotech.api.domain.auth.repository.UserRepository;
import com.nitrotech.api.domain.auth.service.AuthSessionInvalidator;
import com.nitrotech.api.domain.audit.AuditAction;
import com.nitrotech.api.domain.audit.AuditResourceType;
import com.nitrotech.api.domain.audit.dto.AuditLogCommand;
import com.nitrotech.api.domain.audit.service.AuditLogService;
import com.nitrotech.api.domain.user.dto.AdminUserData;
import com.nitrotech.api.domain.user.exception.SelfStatusChangeException;
import com.nitrotech.api.domain.user.repository.AdminUserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Map;

@Service
@RequiredArgsConstructor
public class UpdateAdminUserUseCase {

    private final AdminUserRepository adminUserRepository;
    private final UserRepository userRepository;
    private final AuthSessionInvalidator authSessionInvalidator;
    private final AuditLogService auditLogService;

    @Transactional
    public AdminUserData execute(Long id, String name, String email, String phone, String status, Long currentUserId) {
        AdminUserData current = adminUserRepository.findById(id)
                .orElseThrow(UserNotFoundException::new);
        UserStatus requestedStatus = status == null ? null : UserStatus.fromValue(status);
        if (currentUserId.equals(id) && requestedStatus != null && requestedStatus != UserStatus.active) {
            throw new SelfStatusChangeException();
        }

        String normalizedEmail = email == null ? null : email.trim().toLowerCase();
        if (normalizedEmail != null
                && !normalizedEmail.equals(current.email())
                && userRepository.existsByEmail(normalizedEmail)) {
            throw new EmailAlreadyExistsException(normalizedEmail);
        }

        AdminUserData updated = adminUserRepository.update(
                id,
                name == null ? null : name.trim(),
                normalizedEmail,
                blankToNull(phone),
                status
        );

        if ((normalizedEmail != null && !normalizedEmail.equals(current.email()))
                || (requestedStatus != null && requestedStatus != UserStatus.active)) {
            authSessionInvalidator.invalidateByEmail(current.email());
            if (normalizedEmail != null) {
                authSessionInvalidator.invalidateByEmail(normalizedEmail);
            }
        }
        auditLogService.record(AuditLogCommand.success(
                AuditAction.USER_UPDATED,
                AuditResourceType.USER,
                id,
                Map.of("email", current.email(), "status", current.status()),
                Map.of("email", updated.email(), "status", updated.status()),
                null
        ));
        return updated;
    }

    private String blankToNull(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }
}
