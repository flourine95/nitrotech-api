package com.nitrotech.api.shared.seeder;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.core.annotation.Order;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Slf4j
@Component
@Profile("dev")
@Order(2)
@RequiredArgsConstructor
public class UserDataSeeder implements CommandLineRunner {

    private final JdbcTemplate jdbc;
    private final PasswordEncoder passwordEncoder;

    @Override
    @Transactional
    public void run(String... args) {
        Long existing = jdbc.queryForObject("SELECT COUNT(*) FROM users", Long.class);
        if (existing != null && existing > 0) {
            log.info("Users already exist, skipping seed");
            return;
        }

        List<UserSeed> users = List.of(
                new UserSeed("Admin User", "admin@gmail.com", "0901234567", "admin"),
                new UserSeed("Staff User", "staff@gmail.com", "0901234568", "staff"),
                new UserSeed("Nguyễn Văn An", "customer1@gmail.com", "0901234569", "customer"),
                new UserSeed("Trần Thị Bình", "customer2@gmail.com", "09012345610", "customer"),
                new UserSeed("Lê Văn Cường", "customer3@gmail.com", "09012345611", "customer"),
                new UserSeed("Phạm Thị Dung", "customer4@gmail.com", "09012345612", "customer"),
                new UserSeed("Hoàng Văn Em", "customer5@gmail.com", "09012345613", "customer")
        );

        jdbc.batchUpdate("""
                INSERT INTO users (name, email, password, phone, status, provider)
                VALUES (?, ?, ?, ?, 'active', 'local')
                """, users, users.size(), (ps, user) -> {
            ps.setString(1, user.name());
            ps.setString(2, user.email());
            ps.setString(3, passwordEncoder.encode(user.email()));
            ps.setString(4, user.phone());
        });

        jdbc.batchUpdate("""
                INSERT INTO user_roles (user_id, role_id)
                SELECT u.id, r.id
                FROM users u
                JOIN roles r ON r.slug = ?
                WHERE u.email = ?
                ON CONFLICT DO NOTHING
                """, users, users.size(), (ps, user) -> {
            ps.setString(1, user.roleSlug());
            ps.setString(2, user.email());
        });

        log.info("Seeded {} users (email = password)", users.size());
    }

    private record UserSeed(String name, String email, String phone, String roleSlug) {}
}
