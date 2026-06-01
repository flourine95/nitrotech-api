package com.nitrotech.api.shared.seeder;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.core.annotation.Order;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

@Slf4j
@Component
@Profile("dev")
@Order(8)
@RequiredArgsConstructor
public class ReviewDataSeeder implements CommandLineRunner {

    private static final int TOTAL_REVIEWS = 5000;
    private static final int BATCH_SIZE = 500;

    private final JdbcTemplate jdbc;
    private final Random random = new Random(42);

    private static final String[] POSITIVE_COMMENTS = {
            "Sản phẩm rất tốt, đúng như mô tả!",
            "Chất lượng tuyệt vời, giao hàng nhanh!",
            "Rất hài lòng với sản phẩm này!",
            "Đóng gói cẩn thận, sản phẩm chất lượng!",
            "Giá cả hợp lý, chất lượng tốt!",
            "Sẽ ủng hộ shop lần sau!",
            "Sản phẩm chính hãng, hoạt động tốt!",
            "Đáng đồng tiền bát gạo!",
            "Mình rất thích sản phẩm này!",
            "Shop phục vụ nhiệt tình, sản phẩm ok!"
    };

    private static final String[] NEUTRAL_COMMENTS = {
            "Sản phẩm tạm ổn, giá hơi cao.",
            "Chất lượng bình thường, không có gì đặc biệt.",
            "Giao hàng hơi lâu nhưng sản phẩm ok.",
            "Sản phẩm như mô tả.",
            "Tạm được, giá hơi cao so với chất lượng."
    };

    private static final String[] NEGATIVE_COMMENTS = {
            "Sản phẩm không như mong đợi.",
            "Chất lượng kém, không đáng tiền.",
            "Giao hàng lâu, sản phẩm có vấn đề.",
            "Không giống mô tả, thất vọng.",
            "Sản phẩm bị lỗi, cần đổi trả."
    };

    @Override
    @Transactional
    public void run(String... args) {
        Long existing = jdbc.queryForObject("SELECT COUNT(*) FROM reviews", Long.class);
        if (existing != null && existing > 0) {
            log.info("Reviews already exist, skipping seed");
            return;
        }

        List<Long> productIds = jdbc.queryForList("SELECT id FROM products WHERE deleted_at IS NULL ORDER BY id LIMIT 1000", Long.class);
        List<Long> userIds = jdbc.queryForList("SELECT id FROM users WHERE deleted_at IS NULL ORDER BY id", Long.class);
        List<Long> orderIds = jdbc.queryForList("SELECT id FROM orders WHERE deleted_at IS NULL ORDER BY id", Long.class);

        if (productIds.isEmpty() || userIds.isEmpty() || orderIds.isEmpty()) {
            log.warn("Not enough data (products/users/orders), skipping review seed");
            return;
        }

        List<ReviewSeed> reviews = new ArrayList<>(TOTAL_REVIEWS);
        for (int i = 0; i < TOTAL_REVIEWS; i++) {
            int rating = rating();
            LocalDateTime createdAt = LocalDateTime.now().minusDays(random.nextInt(180));
            reviews.add(new ReviewSeed(
                    productIds.get(random.nextInt(productIds.size())),
                    userIds.get(random.nextInt(userIds.size())),
                    orderIds.get(random.nextInt(orderIds.size())),
                    rating,
                    comment(rating),
                    createdAt
            ));
        }

        jdbc.batchUpdate("""
                INSERT INTO reviews (product_id, user_id, order_id, rating, comment, images, status, created_at, updated_at)
                VALUES (?, ?, ?, ?, ?, NULL, 'approved', ?, ?)
                ON CONFLICT (product_id, user_id, order_id) DO NOTHING
                """, reviews, BATCH_SIZE, (ps, review) -> {
            ps.setLong(1, review.productId());
            ps.setLong(2, review.userId());
            ps.setLong(3, review.orderId());
            ps.setInt(4, review.rating());
            ps.setString(5, review.comment());
            ps.setTimestamp(6, Timestamp.valueOf(review.createdAt()));
            ps.setTimestamp(7, Timestamp.valueOf(review.createdAt()));
        });

        Long inserted = jdbc.queryForObject("SELECT COUNT(*) FROM reviews", Long.class);
        log.info("Seeded {} reviews", inserted);
    }

    private int rating() {
        int rand = random.nextInt(100);
        if (rand < 40) return 5;
        if (rand < 70) return 4;
        if (rand < 85) return 3;
        if (rand < 95) return 2;
        return 1;
    }

    private String comment(int rating) {
        if (rating >= 4) return POSITIVE_COMMENTS[random.nextInt(POSITIVE_COMMENTS.length)];
        if (rating == 3) return NEUTRAL_COMMENTS[random.nextInt(NEUTRAL_COMMENTS.length)];
        return NEGATIVE_COMMENTS[random.nextInt(NEGATIVE_COMMENTS.length)];
    }

    private record ReviewSeed(
            Long productId,
            Long userId,
            Long orderId,
            int rating,
            String comment,
            LocalDateTime createdAt
    ) {}
}
