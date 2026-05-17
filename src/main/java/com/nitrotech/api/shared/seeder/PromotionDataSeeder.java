package com.nitrotech.api.shared.seeder;

import com.nitrotech.api.infrastructure.persistence.entity.PromotionEntity;
import com.nitrotech.api.infrastructure.persistence.repository.PromotionJpaRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Slf4j
@Component
@Profile("dev")
@Order(2)
@RequiredArgsConstructor
public class PromotionDataSeeder implements CommandLineRunner {

    private final PromotionJpaRepository promotionRepo;

    @Override
    public void run(String... args) {
        if (promotionRepo.count() > 0) {
            log.info("Promotions already exist, skipping seed");
            return;
        }

        log.info("Seeding promotions...");

        List<PromotionEntity> promotions = new ArrayList<>();

        PromotionEntity welcome = new PromotionEntity();
        welcome.setName("Chào mừng khách hàng mới");
        welcome.setDescription("Giảm 10% cho đơn hàng đầu tiên");
        welcome.setCode("WELCOME10");
        welcome.setType("percentage");
        welcome.setDiscountValue(new BigDecimal("10"));
        welcome.setMinOrderAmount(new BigDecimal("500000"));
        welcome.setMaxDiscountAmount(new BigDecimal("200000"));
        welcome.setStackable(false);
        welcome.setPriority(10);
        welcome.setUsageLimit(1000);
        welcome.setUsagePerUser(1);
        welcome.setStartAt(LocalDateTime.now());
        welcome.setEndAt(LocalDateTime.now().plusMonths(6));
        welcome.setStatus("active");
        promotions.add(welcome);

        PromotionEntity freeship = new PromotionEntity();
        freeship.setName("Miễn phí vận chuyển");
        freeship.setDescription("Miễn phí ship cho đơn từ 1 triệu");
        freeship.setCode("FREESHIP");
        freeship.setType("fixed");
        freeship.setDiscountValue(new BigDecimal("30000"));
        freeship.setMinOrderAmount(new BigDecimal("1000000"));
        freeship.setMaxDiscountAmount(new BigDecimal("30000"));
        freeship.setStackable(true);
        freeship.setPriority(5);
        freeship.setUsageLimit(null);
        freeship.setUsagePerUser(10);
        freeship.setStartAt(LocalDateTime.now());
        freeship.setEndAt(LocalDateTime.now().plusMonths(3));
        freeship.setStatus("active");
        promotions.add(freeship);

        PromotionEntity summer = new PromotionEntity();
        summer.setName("Khuyến mãi hè 2026");
        summer.setDescription("Giảm 500k cho đơn từ 5 triệu");
        summer.setCode("SUMMER2026");
        summer.setType("fixed");
        summer.setDiscountValue(new BigDecimal("500000"));
        summer.setMinOrderAmount(new BigDecimal("5000000"));
        summer.setMaxDiscountAmount(new BigDecimal("500000"));
        summer.setStackable(false);
        summer.setPriority(20);
        summer.setUsageLimit(500);
        summer.setUsagePerUser(1);
        summer.setStartAt(LocalDateTime.now());
        summer.setEndAt(LocalDateTime.now().plusMonths(2));
        summer.setStatus("active");
        promotions.add(summer);

        PromotionEntity vip = new PromotionEntity();
        vip.setName("Ưu đãi khách hàng VIP");
        vip.setDescription("Giảm 15% cho đơn từ 10 triệu");
        vip.setCode("VIP15");
        vip.setType("percentage");
        vip.setDiscountValue(new BigDecimal("15"));
        vip.setMinOrderAmount(new BigDecimal("10000000"));
        vip.setMaxDiscountAmount(new BigDecimal("2000000"));
        vip.setStackable(false);
        vip.setPriority(30);
        vip.setUsageLimit(100);
        vip.setUsagePerUser(3);
        vip.setStartAt(LocalDateTime.now());
        vip.setEndAt(LocalDateTime.now().plusYears(1));
        vip.setStatus("active");
        promotions.add(vip);

        PromotionEntity flash = new PromotionEntity();
        flash.setName("Flash Sale cuối tuần");
        flash.setDescription("Giảm 200k cho đơn từ 2 triệu");
        flash.setCode("FLASH200");
        flash.setType("fixed");
        flash.setDiscountValue(new BigDecimal("200000"));
        flash.setMinOrderAmount(new BigDecimal("2000000"));
        flash.setMaxDiscountAmount(new BigDecimal("200000"));
        flash.setStackable(true);
        flash.setPriority(15);
        flash.setUsageLimit(200);
        flash.setUsagePerUser(1);
        flash.setStartAt(LocalDateTime.now().plusDays(5));
        flash.setEndAt(LocalDateTime.now().plusDays(7));
        flash.setStatus("scheduled");
        promotions.add(flash);

        promotionRepo.saveAll(promotions);

        log.info("Seeded {} promotions", promotions.size());
    }
}
