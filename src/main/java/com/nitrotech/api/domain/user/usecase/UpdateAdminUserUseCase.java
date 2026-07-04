package com.nitrotech.api.domain.user.usecase;

import com.nitrotech.api.domain.auth.exception.EmailAlreadyExistsException;
import com.nitrotech.api.domain.auth.exception.UserNotFoundException;
import com.nitrotech.api.domain.user.dto.AdminUserData;
import com.nitrotech.api.domain.user.repository.AdminUserRepository;
import com.nitrotech.api.infrastructure.persistence.repository.UserJpaRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.session.FindByIndexNameSessionRepository;
import org.springframework.session.Session;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class UpdateAdminUserUseCase {

    private final AdminUserRepository adminUserRepository;
    private final UserJpaRepository userJpaRepository;
    private final FindByIndexNameSessionRepository<? extends Session> sessionRepository;

    @Transactional
    public AdminUserData execute(Long id, String name, String email, String phone, String status, Long currentUserId) {
        AdminUserData current = adminUserRepository.findById(id)
                .orElseThrow(UserNotFoundException::new);
        if (currentUserId.equals(id) && status != null && !"active".equals(status)) {
            throw new com.nitrotech.api.shared.exception.ForbiddenException(
                    "SELF_STATUS_CHANGE", "Không thể tự khóa tài khoản của chính mình");
        }

        String normalizedEmail = email == null ? null : email.trim().toLowerCase();
        if (normalizedEmail != null
                && !normalizedEmail.equals(current.email())
                && userJpaRepository.existsByEmail(normalizedEmail)) {
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
                || (status != null && !"active".equals(status))) {
            invalidateSessions(current.email());
            if (normalizedEmail != null) {
                invalidateSessions(normalizedEmail);
            }
        }
        return updated;
    }

    private void invalidateSessions(String email) {
        try {
            sessionRepository.findByPrincipalName(email)
                    .values()
                    .forEach(session -> sessionRepository.deleteById(session.getId()));
        } catch (Exception ignored) {}
    }

    private String blankToNull(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }
}
