package com.nitrotech.api.domain.auth.usecase;

import com.nitrotech.api.domain.auth.dto.UserProfileData;
import com.nitrotech.api.domain.auth.repository.UserRepository;
import com.nitrotech.api.shared.exception.NotFoundException;
import org.springframework.stereotype.Service;

@Service
public class GetProfileUseCase {

    private final UserRepository userRepository;

    public GetProfileUseCase(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    public UserProfileData execute(Long userId) {
        return userRepository.findById(userId)
                .orElseThrow(() -> new NotFoundException("USER_NOT_FOUND", "User not found"));
    }

    public UserProfileData executeByEmail(String email) {
        return userRepository.findByEmail(email)
                .orElseThrow(() -> new NotFoundException("USER_NOT_FOUND", "User not found"));
    }
}
