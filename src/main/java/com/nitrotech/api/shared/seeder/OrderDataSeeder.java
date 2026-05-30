package com.nitrotech.api.shared.seeder;

import com.nitrotech.api.infrastructure.persistence.entity.OrderEntity;
import com.nitrotech.api.infrastructure.persistence.entity.OrderItemEntity;
import com.nitrotech.api.infrastructure.persistence.entity.UserEntity;
import com.nitrotech.api.infrastructure.persistence.repository.OrderItemJpaRepository;
import com.nitrotech.api.infrastructure.persistence.repository.OrderJpaRepository;
import com.nitrotech.api.infrastructure.persistence.repository.ProductVariantJpaRepository;
import com.nitrotech.api.infrastructure.persistence.repository.UserJpaRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.*;

@Slf4j
@Component
@Profile("dev")
@Order(2) // Run after users (1), before products (3)
@RequiredArgsConstructor
public class OrderDataSeeder implements CommandLineRunner {

    private static final int TOTAL_ORDERS = 200;

    private final OrderJpaRepository orderJpa;
    private final OrderItemJpaRepository orderItemJpa;
    private final UserJpaRepository userJpa;
    private final ProductVariantJpaRepository variantJpa;

    private final Random random = new Random(42);

    private static final String[] STATUSES = {"delivered", "delivered", "delivered", "shipped", "processing"};

    @Override
    public void run(String... args) {
        if (orderJpa.count() > 0) {
            log.info("Orders already exist, skipping seed");
            return;
        }

        log.info("Seeding {} orders...", TOTAL_ORDERS);

        List<Long> userIds = userJpa.findAll().stream()
                .map(UserEntity::getId)
                .toList();

        List<Long> variantIds = variantJpa.findAll().stream()
                .map(v -> v.getId())
                .limit(1000)
                .toList();

        if (userIds.isEmpty() || variantIds.isEmpty()) {
            log.warn("Not enough users or variants, skipping order seed");
            return;
        }

        for (int i = 0; i < TOTAL_ORDERS; i++) {
            seedOrder(i, userIds, variantIds);
        }

        log.info("Seeding completed: {} orders", TOTAL_ORDERS);
    }

    @Transactional
    public void seedOrder(int index, List<Long> userIds, List<Long> variantIds) {
        Long userId = userIds.get(random.nextInt(userIds.size()));

        // Create order
        OrderEntity order = new OrderEntity();
        order.setUserId(userId);
        order.setShippingAddress(generateAddress());
        order.setStatus(STATUSES[random.nextInt(STATUSES.length)]);
        order.setPaymentMethod("cod");
        order.setTotalAmount(BigDecimal.ZERO);
        order.setDiscountAmount(BigDecimal.ZERO);
        order.setShippingFee(BigDecimal.valueOf(30000));
        order.setFinalAmount(BigDecimal.ZERO);
        order.setCreatedAt(generateRandomDate());
        order.setUpdatedAt(order.getCreatedAt());

        OrderEntity savedOrder = orderJpa.save(order);

        // Create 1-3 order items
        int itemCount = 1 + random.nextInt(3);
        BigDecimal totalAmount = BigDecimal.ZERO;

        for (int i = 0; i < itemCount; i++) {
            Long variantId = variantIds.get(random.nextInt(variantIds.size()));
            int quantity = 1 + random.nextInt(2);
            BigDecimal unitPrice = BigDecimal.valueOf(500000 + random.nextInt(50000000));
            BigDecimal subtotal = unitPrice.multiply(BigDecimal.valueOf(quantity));

            OrderItemEntity item = new OrderItemEntity();
            item.setOrder(savedOrder);
            item.setVariantId(variantId);
            item.setName("Product Item " + i);
            item.setSku("SKU-" + index + "-" + i);
            item.setQuantity(quantity);
            item.setUnitPrice(unitPrice);
            item.setSubtotal(subtotal);
            orderItemJpa.save(item);

            totalAmount = totalAmount.add(subtotal);
        }

        // Update order total
        savedOrder.setTotalAmount(totalAmount);
        savedOrder.setFinalAmount(totalAmount.add(savedOrder.getShippingFee()));
        orderJpa.save(savedOrder);
    }

    private Map<String, Object> generateAddress() {
        Map<String, Object> address = new HashMap<>();
        address.put("name", "Customer " + random.nextInt(1000));
        address.put("phone", "0" + (900000000 + random.nextInt(100000000)));
        address.put("address", random.nextInt(500) + " Main Street");
        address.put("ward", "Ward " + (1 + random.nextInt(20)));
        address.put("district", "District " + (1 + random.nextInt(12)));
        address.put("city", "Ho Chi Minh");
        address.put("country", "Vietnam");
        return address;
    }

    private LocalDateTime generateRandomDate() {
        // Random date within last 6 months
        int daysAgo = random.nextInt(180);
        return LocalDateTime.now().minusDays(daysAgo);
    }
}
