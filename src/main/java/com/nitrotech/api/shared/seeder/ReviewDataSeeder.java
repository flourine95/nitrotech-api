package com.nitrotech.api.shared.seeder;

import com.nitrotech.api.infrastructure.persistence.entity.ReviewEntity;
import com.nitrotech.api.infrastructure.persistence.repository.OrderJpaRepository;
import com.nitrotech.api.infrastructure.persistence.repository.ProductJpaRepository;
import com.nitrotech.api.infrastructure.persistence.repository.ReviewJpaRepository;
import com.nitrotech.api.infrastructure.persistence.repository.UserJpaRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

@Slf4j
@Component
@Profile("dev")
@Order(4)
@RequiredArgsConstructor
public class ReviewDataSeeder implements CommandLineRunner {

    private static final int TOTAL_REVIEWS = 5000;
    private static final int CHUNK_SIZE = 500;

    private final ReviewJpaRepository reviewJpa;
    private final ProductJpaRepository productJpa;
    private final UserJpaRepository userJpa;
    private final OrderJpaRepository orderJpa;

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
    public void run(String... args) {
        if (reviewJpa.count() > 0) {
            log.info("Reviews already exist, skipping seed");
            return;
        }

        log.info("Seeding {} reviews...", TOTAL_REVIEWS);

        // Get available products, users, orders
        List<Long> productIds = productJpa.findAll().stream()
                .map(p -> p.getId())
                .limit(1000)
                .toList();
        
        List<Long> userIds = userJpa.findAll().stream()
                .map(u -> u.getId())
                .toList();
        
        List<Long> orderIds = orderJpa.findAll().stream()
                .map(o -> o.getId())
                .toList();

        if (productIds.isEmpty() || userIds.isEmpty() || orderIds.isEmpty()) {
            log.warn("Not enough data (products/users/orders), skipping review seed");
            return;
        }

        for (int chunk = 0; chunk < TOTAL_REVIEWS; chunk += CHUNK_SIZE) {
            int batchSize = Math.min(CHUNK_SIZE, TOTAL_REVIEWS - chunk);
            seedChunk(chunk, batchSize, productIds, userIds, orderIds);
        }

        log.info("Seeding completed: {} reviews", TOTAL_REVIEWS);
    }

    @Transactional
    public void seedChunk(int offset, int count, List<Long> productIds, List<Long> userIds, List<Long> orderIds) {
        List<ReviewEntity> reviews = new ArrayList<>(count);

        for (int i = 0; i < count; i++) {
            Long productId = productIds.get(random.nextInt(productIds.size()));
            Long userId = userIds.get(random.nextInt(userIds.size()));
            Long orderId = orderIds.get(random.nextInt(orderIds.size()));

            // Generate rating (weighted towards positive)
            int rating = generateRating();
            String comment = generateComment(rating);

            ReviewEntity review = new ReviewEntity();
            review.setProductId(productId);
            review.setUserId(userId);
            review.setOrderId(orderId);
            review.setRating((short) rating);
            review.setComment(comment);
            review.setStatus("approved"); // Auto-approve for seed data
            review.setCreatedAt(generateRandomDate());
            review.setUpdatedAt(review.getCreatedAt());

            reviews.add(review);
        }

        try {
            reviewJpa.saveAll(reviews);
        } catch (Exception e) {
            // Ignore duplicate key errors (unique constraint on product_id, user_id, order_id)
            log.debug("Some reviews skipped due to duplicate constraint");
        }
    }

    private int generateRating() {
        // Weighted distribution: more 4-5 stars
        int rand = random.nextInt(100);
        if (rand < 40) return 5;      // 40% - 5 stars
        if (rand < 70) return 4;      // 30% - 4 stars
        if (rand < 85) return 3;      // 15% - 3 stars
        if (rand < 95) return 2;      // 10% - 2 stars
        return 1;                     // 5% - 1 star
    }

    private String generateComment(int rating) {
        if (rating >= 4) {
            return POSITIVE_COMMENTS[random.nextInt(POSITIVE_COMMENTS.length)];
        } else if (rating == 3) {
            return NEUTRAL_COMMENTS[random.nextInt(NEUTRAL_COMMENTS.length)];
        } else {
            return NEGATIVE_COMMENTS[random.nextInt(NEGATIVE_COMMENTS.length)];
        }
    }

    private LocalDateTime generateRandomDate() {
        // Random date within last 6 months
        int daysAgo = random.nextInt(180);
        return LocalDateTime.now().minusDays(daysAgo);
    }
}
