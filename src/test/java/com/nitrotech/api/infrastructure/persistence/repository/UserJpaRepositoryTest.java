package com.nitrotech.api.infrastructure.persistence.repository;

import com.nitrotech.api.infrastructure.persistence.entity.UserEntity;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@Transactional
class UserJpaRepositoryTest {

    @Autowired
    private UserJpaRepository userRepository;

    @Test
    void emailExistsCheckIncludesSoftDeletedUsersBecauseEmailIsGloballyUnique() {
        UserEntity user = new UserEntity();
        user.setName("Deleted User");
        user.setEmail("deleted-user@example.com");
        user.setDeletedAt(Instant.now());
        userRepository.saveAndFlush(user);

        assertThat(userRepository.existsByEmail("deleted-user@example.com")).isTrue();
    }

    @Test
    void emailLookupForLoginIgnoresSoftDeletedUsers() {
        UserEntity user = new UserEntity();
        user.setName("Deleted User");
        user.setEmail("deleted-login@example.com");
        user.setDeletedAt(Instant.now());
        userRepository.saveAndFlush(user);

        assertThat(userRepository.findByEmail("deleted-login@example.com")).isEmpty();
    }
}
