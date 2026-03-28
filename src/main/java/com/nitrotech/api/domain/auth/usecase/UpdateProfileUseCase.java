package com.nitrotech.api.domain.auth.usecase;

import com.nitrotech.api.domain.auth.dto.UpdateProfileCommand;
import com.nitrotech.api.domain.auth.dto.UserProfileData;
import com.nitrotech.api.domain.auth.repository.UserRepository;
import org.springframework.stereotype.Service;

@Service
public class UpdateProfileUseCase {

    private final UserRepository userRepository;

    public UpdateProfileUseCase(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    public UserProfileData execute(UpdateProfileCommand command) {
        return userRepository.updateProfile(
                command.userId(),
                command.name(),
                command.phone(),
                command.avatar()
        );
    }
}
