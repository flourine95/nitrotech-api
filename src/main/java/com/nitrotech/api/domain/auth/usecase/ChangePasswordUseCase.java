package com.nitrotech.api.domain.auth.usecase;

import com.nitrotech.api.domain.auth.dto.ChangePasswordCommand;
import com.nitrotech.api.domain.auth.exception.InvalidCredentialsException;
import com.nitrotech.api.domain.auth.repository.UserRepository;
import com.nitrotech.api.shared.exception.NotFoundException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

@Service
public class ChangePasswordUseCase {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    public ChangePasswordUseCase(UserRepository userRepository, PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
    }

    public void execute(ChangePasswordCommand command) {
        UserRepository.UserCredential credential = userRepository.findCredentialByEmail(
                userRepository.findById(command.userId())
                        .orElseThrow(() -> new NotFoundException("USER_NOT_FOUND", "User not found"))
                        .email()
        ).orElseThrow(InvalidCredentialsException::new);

        if (!passwordEncoder.matches(command.currentPassword(), credential.hashedPassword())) {
            throw new InvalidCredentialsException();
        }

        userRepository.updatePassword(command.userId(), passwordEncoder.encode(command.newPassword()));
    }
}
