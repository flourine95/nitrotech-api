package com.nitrotech.api.domain.auth.usecase;

import com.nitrotech.api.domain.auth.exception.InvalidVerificationTokenException;
import com.nitrotech.api.domain.auth.repository.EmailVerificationTokenRepository;
import com.nitrotech.api.domain.auth.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class VerifyEmailUseCase {

    private final EmailVerificationTokenRepository verificationTokenRepository;
    private final UserRepository userRepository;

    @Transactional
    public void execute(String token) {
        EmailVerificationTokenRepository.VerificationToken verificationToken =
                verificationTokenRepository.findValidVerification(token)
                        .orElseThrow(InvalidVerificationTokenException::new);

        userRepository.activateUser(verificationToken.userId());
        verificationTokenRepository.markVerificationUsed(token);
    }
}
