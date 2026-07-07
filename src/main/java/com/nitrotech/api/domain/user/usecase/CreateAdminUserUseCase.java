package com.nitrotech.api.domain.user.usecase;

import com.nitrotech.api.domain.auth.exception.EmailAlreadyExistsException;
import com.nitrotech.api.domain.auth.repository.UserRepository;
import com.nitrotech.api.domain.auth.usecase.ForgotPasswordUseCase;
import com.nitrotech.api.domain.access.exception.RoleReferenceNotFoundException;
import com.nitrotech.api.domain.access.repository.AccessManagementRepository;
import com.nitrotech.api.domain.audit.AuditAction;
import com.nitrotech.api.domain.audit.AuditResourceType;
import com.nitrotech.api.domain.audit.dto.AuditLogCommand;
import com.nitrotech.api.domain.audit.service.AuditLogService;
import com.nitrotech.api.domain.user.dto.AdminUserData;
import com.nitrotech.api.domain.user.repository.AdminUserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Map;
import java.util.Set;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class CreateAdminUserUseCase {

    private final AdminUserRepository adminUserRepository;
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final ForgotPasswordUseCase forgotPasswordUseCase;
    private final AccessManagementRepository accessManagementRepository;
    private final AuditLogService auditLogService;

    @Transactional
    public AdminUserData execute(String name, String email, String phone, String status, Set<String> roleSlugs) {
        String normalizedEmail = email.trim().toLowerCase();
        Set<String> roles = roleSlugs == null || roleSlugs.isEmpty() ? Set.of("customer") : roleSlugs;
        if (userRepository.existsByEmail(normalizedEmail)) {
            throw new EmailAlreadyExistsException(normalizedEmail);
        }
        if (accessManagementRepository.findRoleIdsBySlugs(roles).size() != roles.size()) {
            throw new RoleReferenceNotFoundException();
        }

        AdminUserData user = adminUserRepository.create(
                name.trim(),
                normalizedEmail,
                blankToNull(phone),
                status,
                passwordEncoder.encode(UUID.randomUUID().toString()),
                roles
        );
        auditLogService.record(AuditLogCommand.success(
                AuditAction.USER_CREATED,
                AuditResourceType.USER,
                user.id(),
                null,
                Map.of("email", user.email(), "status", user.status(), "roleSlugs", user.roleSlugs()),
                null
        ));
        forgotPasswordUseCase.execute(normalizedEmail);
        return user;
    }

    private String blankToNull(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }
}
