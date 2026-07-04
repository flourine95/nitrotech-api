package com.nitrotech.api.domain.user.usecase;

import com.nitrotech.api.domain.auth.exception.UserNotFoundException;
import com.nitrotech.api.domain.auth.usecase.ForgotPasswordUseCase;
import com.nitrotech.api.domain.user.repository.AdminUserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.session.FindByIndexNameSessionRepository;
import org.springframework.session.Session;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class ResetAdminUserPasswordUseCase {

    private final AdminUserRepository adminUserRepository;
    private final ForgotPasswordUseCase forgotPasswordUseCase;
    private final FindByIndexNameSessionRepository<? extends Session> sessionRepository;

    public void execute(Long id) {
        String email = adminUserRepository.findById(id)
                .orElseThrow(UserNotFoundException::new)
                .email();
        forgotPasswordUseCase.execute(email);
        try {
            sessionRepository.findByPrincipalName(email)
                    .values()
                    .forEach(session -> sessionRepository.deleteById(session.getId()));
        } catch (Exception ignored) {}
    }
}
