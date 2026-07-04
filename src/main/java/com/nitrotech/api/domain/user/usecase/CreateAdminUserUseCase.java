package com.nitrotech.api.domain.user.usecase;

import com.nitrotech.api.domain.auth.exception.EmailAlreadyExistsException;
import com.nitrotech.api.domain.auth.usecase.ForgotPasswordUseCase;
import com.nitrotech.api.domain.access.repository.AccessManagementRepository;
import com.nitrotech.api.domain.user.dto.AdminUserData;
import com.nitrotech.api.domain.user.repository.AdminUserRepository;
import com.nitrotech.api.infrastructure.persistence.repository.UserJpaRepository;
import com.nitrotech.api.shared.exception.BadRequestException;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.Set;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class CreateAdminUserUseCase {

    private final AdminUserRepository adminUserRepository;
    private final UserJpaRepository userJpaRepository;
    private final PasswordEncoder passwordEncoder;
    private final ForgotPasswordUseCase forgotPasswordUseCase;
    private final AccessManagementRepository accessManagementRepository;

    public AdminUserData execute(String name, String email, String phone, String status, Set<String> roleSlugs) {
        String normalizedEmail = email.trim().toLowerCase();
        Set<String> roles = roleSlugs == null || roleSlugs.isEmpty() ? Set.of("customer") : roleSlugs;
        if (userJpaRepository.existsByEmail(normalizedEmail)) {
            throw new EmailAlreadyExistsException(normalizedEmail);
        }
        if (accessManagementRepository.findRoleIdsBySlugs(roles).size() != roles.size()) {
            throw new BadRequestException("ROLE_NOT_FOUND", "One or more roles were not found");
        }

        AdminUserData user = adminUserRepository.create(
                name.trim(),
                normalizedEmail,
                blankToNull(phone),
                status,
                passwordEncoder.encode(UUID.randomUUID().toString()),
                roles
        );
        forgotPasswordUseCase.execute(normalizedEmail);
        return user;
    }

    private String blankToNull(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }
}
