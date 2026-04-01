package com.nitrotech.api.domain.auth.usecase;

import com.nitrotech.api.domain.auth.exception.InvalidVerificationTokenException;
import com.nitrotech.api.domain.auth.repository.EmailVerificationTokenRepository;
import com.nitrotech.api.domain.auth.repository.UserRepository;
import org.springframework.stereotype.Service;

@Service
public class VerifyEmailUseCase {

    private final EmailVerificationTokenRepository verificationTokenRepository;
    private final UserRepository userRepository;

    public VerifyEmailUseCase(EmailVerificationTokenRepository verificationTokenRepository,
                               UserRepository userRepository) {
        this.verificationTokenRepository = verificationTokenRepository;
        this.userRepository = userRepository;
    }

    public void execute(String token) {
        EmailVerificationTokenRepository.VerificationToken verificationToken =
                verificationTokenRepository.findValidVerification(token)
                        .orElseThrow(InvalidVerificationTokenException::new);

        userRepository.activateUser(verificationToken.userId());
        verificationTokenRepository.markVerificationUsed(token);
    }
}
