package com.nitrotech.api.shared.seeder;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.core.annotation.Order;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.List;

@Slf4j
@Component
@Profile("dev")
@Order(3)
@RequiredArgsConstructor
public class PromotionDataSeeder implements CommandLineRunner {

    private final JdbcTemplate jdbc;

    @Override
    @Transactional
    public void run(String... args) {
        Long existing = jdbc.queryForObject("SELECT COUNT(*) FROM promotions", Long.class);
        if (existing != null && existing > 0) {
            log.info("Promotions already exist, skipping seed");
            return;
        }

        LocalDateTime now = LocalDateTime.now();
        List<PromotionSeed> promotions = List.of(
                new PromotionSeed("Chào mừng khách hàng mới", "Giảm 10% cho đơn hàng đầu tiên", "WELCOME10",
                        "percentage", bd("10"), bd("500000"), bd("200000"), false, 10, 1000, 1, now, now.plusMonths(6), "active"),
                new PromotionSeed("Miễn phí vận chuyển", "Miễn phí ship cho đơn từ 1 triệu", "FREESHIP",
                        "fixed", bd("30000"), bd("1000000"), bd("30000"), true, 5, null, 10, now, now.plusMonths(3), "active"),
                new PromotionSeed("Khuyến mãi hè 2026", "Giảm 500k cho đơn từ 5 triệu", "SUMMER2026",
                        "fixed", bd("500000"), bd("5000000"), bd("500000"), false, 20, 500, 1, now, now.plusMonths(2), "active"),
                new PromotionSeed("Ưu đãi khách hàng VIP", "Giảm 15% cho đơn từ 10 triệu", "VIP15",
                        "percentage", bd("15"), bd("10000000"), bd("2000000"), false, 30, 100, 3, now, now.plusYears(1), "active"),
                new PromotionSeed("Flash Sale cuối tuần", "Giảm 200k cho đơn từ 2 triệu", "FLASH200",
                        "fixed", bd("200000"), bd("2000000"), bd("200000"), true, 15, 200, 1, now.plusDays(5), now.plusDays(7), "scheduled")
        );

        jdbc.batchUpdate("""
                INSERT INTO promotions (
                    name, description, code, type, discount_value, min_order_amount,
                    max_discount_amount, stackable, priority, usage_limit, usage_per_user,
                    start_at, end_at, status
                )
                VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
                """, promotions, promotions.size(), (ps, p) -> {
            ps.setString(1, p.name());
            ps.setString(2, p.description());
            ps.setString(3, p.code());
            ps.setString(4, p.type());
            ps.setBigDecimal(5, p.discountValue());
            ps.setBigDecimal(6, p.minOrderAmount());
            ps.setBigDecimal(7, p.maxDiscountAmount());
            ps.setBoolean(8, p.stackable());
            ps.setInt(9, p.priority());
            if (p.usageLimit() == null) ps.setObject(10, null);
            else ps.setInt(10, p.usageLimit());
            ps.setInt(11, p.usagePerUser());
            ps.setTimestamp(12, Timestamp.valueOf(p.startAt()));
            ps.setTimestamp(13, Timestamp.valueOf(p.endAt()));
            ps.setString(14, p.status());
        });

        log.info("Seeded {} promotions", promotions.size());
    }

    private BigDecimal bd(String value) {
        return new BigDecimal(value);
    }

    private record PromotionSeed(
            String name,
            String description,
            String code,
            String type,
            BigDecimal discountValue,
            BigDecimal minOrderAmount,
            BigDecimal maxDiscountAmount,
            boolean stackable,
            int priority,
            Integer usageLimit,
            int usagePerUser,
            LocalDateTime startAt,
            LocalDateTime endAt,
            String status
    ) {}
}
