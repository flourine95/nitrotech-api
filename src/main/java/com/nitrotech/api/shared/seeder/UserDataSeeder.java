package com.nitrotech.api.shared.seeder;

import com.nitrotech.api.infrastructure.persistence.entity.UserEntity;
import com.nitrotech.api.infrastructure.persistence.repository.UserJpaRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.core.annotation.Order;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

@Slf4j
@Component
@Profile("dev")
@Order(1)
@RequiredArgsConstructor
public class UserDataSeeder implements CommandLineRunner {

    private final UserJpaRepository userRepo;
    private final PasswordEncoder passwordEncoder;

    @Override
    public void run(String... args) {
        if (userRepo.count() > 0) {
            log.info("Users already exist, skipping seed");
            return;
        }

        log.info("Seeding users...");

        List<UserEntity> users = new ArrayList<>();

        UserEntity admin = new UserEntity();
        admin.setName("Admin User");
        admin.setEmail("admin@gmail.com");
        admin.setPassword(passwordEncoder.encode("admin@gmail.com"));
        admin.setPhone("0901234567");
        admin.setStatus(UserEntity.Status.active);
        admin.setProvider(UserEntity.Provider.local);
        users.add(admin);

        UserEntity staff = new UserEntity();
        staff.setName("Staff User");
        staff.setEmail("staff@gmail.com");
        staff.setPassword(passwordEncoder.encode("staff@gmail.com"));
        staff.setPhone("0901234568");
        staff.setStatus(UserEntity.Status.active);
        staff.setProvider(UserEntity.Provider.local);
        users.add(staff);

        String[] customerNames = {
            "Nguyễn Văn An", "Trần Thị Bình", "Lê Văn Cường",
            "Phạm Thị Dung", "Hoàng Văn Em"
        };

        for (int i = 0; i < customerNames.length; i++) {
            String email = "customer" + (i + 1) + "@gmail.com";
            UserEntity customer = new UserEntity();
            customer.setName(customerNames[i]);
            customer.setEmail(email);
            customer.setPassword(passwordEncoder.encode(email));
            customer.setPhone("090123456" + (9 + i));
            customer.setStatus(UserEntity.Status.active);
            customer.setProvider(UserEntity.Provider.local);
            users.add(customer);
        }

        userRepo.saveAll(users);

        log.info("Seeded {} users (email = password)", users.size());
    }
}
