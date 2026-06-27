package com.nitrotech.api.domain.auth.usecase;

import com.nitrotech.api.domain.auth.exception.UserNotFoundException;

import com.nitrotech.api.domain.auth.dto.UserProfileData;
import com.nitrotech.api.domain.auth.repository.UserRepository;
import com.nitrotech.api.shared.exception.NotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class GetProfileUseCase {

    private final UserRepository userRepository;

    public UserProfileData execute(Long userId) {
        return userRepository.findById(userId)
                .orElseThrow(() -> new UserNotFoundException());
    }

    public UserProfileData executeByEmail(String email) {
        return userRepository.findByEmail(email)
                .orElseThrow(() -> new UserNotFoundException());
    }
}
