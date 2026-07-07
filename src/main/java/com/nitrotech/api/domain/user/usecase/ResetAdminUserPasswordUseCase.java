package com.nitrotech.api.domain.user.usecase;

import com.nitrotech.api.domain.auth.exception.UserNotFoundException;
import com.nitrotech.api.domain.auth.service.AuthSessionInvalidator;
import com.nitrotech.api.domain.auth.usecase.ForgotPasswordUseCase;
import com.nitrotech.api.domain.audit.AuditAction;
import com.nitrotech.api.domain.audit.AuditResourceType;
import com.nitrotech.api.domain.audit.dto.AuditLogCommand;
import com.nitrotech.api.domain.audit.service.AuditLogService;
import com.nitrotech.api.domain.user.repository.AdminUserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Map;

@Service
@RequiredArgsConstructor
public class ResetAdminUserPasswordUseCase {

    private final AdminUserRepository adminUserRepository;
    private final ForgotPasswordUseCase forgotPasswordUseCase;
    private final AuthSessionInvalidator authSessionInvalidator;
    private final AuditLogService auditLogService;

    @Transactional
    public void execute(Long id) {
        String email = adminUserRepository.findById(id)
                .orElseThrow(UserNotFoundException::new)
                .email();
        forgotPasswordUseCase.execute(email);
        authSessionInvalidator.invalidateByEmail(email);
        auditLogService.record(AuditLogCommand.success(
                AuditAction.USER_PASSWORD_RESET,
                AuditResourceType.USER,
                id,
                null,
                null,
                Map.of("targetEmail", email)
        ));
    }
}
