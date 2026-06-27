package com.nitrotech.api.domain.auth.usecase;

import com.nitrotech.api.domain.auth.exception.UserNotFoundException;

import com.nitrotech.api.domain.auth.dto.ChangePasswordCommand;
import com.nitrotech.api.domain.auth.exception.InvalidCredentialsException;
import com.nitrotech.api.domain.auth.repository.UserRepository;
import com.nitrotech.api.shared.exception.NotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class ChangePasswordUseCase {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    public void execute(ChangePasswordCommand command) {
        UserRepository.UserCredential credential = userRepository.findCredentialByEmail(
                userRepository.findById(command.userId())
                        .orElseThrow(() -> new UserNotFoundException())
                        .email()
        ).orElseThrow(InvalidCredentialsException::new);

        if (!passwordEncoder.matches(command.currentPassword(), credential.hashedPassword())) {
            throw new InvalidCredentialsException();
        }

        userRepository.updatePassword(command.userId(), passwordEncoder.encode(command.newPassword()));
    }
}
