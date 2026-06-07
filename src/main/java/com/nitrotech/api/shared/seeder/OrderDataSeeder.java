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
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;

@Slf4j
@Component
@Profile("dev")
@Order(7)
@RequiredArgsConstructor
public class OrderDataSeeder implements CommandLineRunner {

    private static final int TOTAL_ORDERS = 200;
    private static final int BATCH_SIZE = 200;

    private final JdbcTemplate jdbc;
    private final Random random = new Random(42);

    private static final String[] STATUSES = {"delivered", "delivered", "delivered", "shipped", "processing"};

    @Override
    @Transactional
    public void run(String... args) {
        Long existing = jdbc.queryForObject("SELECT COUNT(*) FROM orders", Long.class);
        if (existing != null && existing > 0) {
            log.info("Orders already exist, skipping seed");
            return;
        }

        List<Long> userIds = jdbc.queryForList("SELECT id FROM users WHERE deleted_at IS NULL", Long.class);
        List<VariantRef> variants = jdbc.query("""
                        SELECT id, sku, name, price
                        FROM product_variants
                        WHERE deleted_at IS NULL
                        ORDER BY id
                        LIMIT 1000
                        """,
                (rs, rowNum) -> new VariantRef(
                        rs.getLong("id"),
                        rs.getString("sku"),
                        rs.getString("name"),
                        rs.getLong("price")
                ));

        if (userIds.isEmpty() || variants.isEmpty()) {
            log.warn("Not enough users or variants, skipping order seed");
            return;
        }

        List<OrderSeed> orders = buildOrders(userIds, variants);
        insertOrders(orders);

        Map<String, Long> orderIds = loadSeedOrderIds();
        List<OrderItemSeed> items = buildOrderItems(orders, orderIds, variants);
        insertOrderItems(items);

        log.info("Seeded {} orders and {} order items", orders.size(), items.size());
    }

    private List<OrderSeed> buildOrders(List<Long> userIds, List<VariantRef> variants) {
        List<OrderSeed> orders = new ArrayList<>(TOTAL_ORDERS);
        for (int i = 0; i < TOTAL_ORDERS; i++) {
            long total = 0;
            for (ItemDraft item : itemDrafts(i, variants)) {
                total += item.variant().price() * item.quantity();
            }

            long shippingFee = 30_000;
            LocalDateTime createdAt = randomDate();
            orders.add(new OrderSeed(
                    i,
                    userIds.get(random.nextInt(userIds.size())),
                    addressJson(),
                    STATUSES[random.nextInt(STATUSES.length)],
                    total,
                    shippingFee,
                    total + shippingFee,
                    "seed-order-" + i,
                    createdAt
            ));
        }
        return orders;
    }

    private void insertOrders(List<OrderSeed> orders) {
        jdbc.batchUpdate("""
                INSERT INTO orders (
                    user_id, shipping_address, status, payment_method, total_amount,
                    discount_amount, shipping_fee, final_amount, note, created_at, updated_at
                )
                VALUES (?, ?::jsonb, ?, 'cod', ?, 0, ?, ?, ?, ?, ?)
                """, orders, BATCH_SIZE, (ps, order) -> {
            ps.setLong(1, order.userId());
            ps.setString(2, order.shippingAddressJson());
            ps.setString(3, order.status());
            ps.setLong(4, order.totalAmount());
            ps.setLong(5, order.shippingFee());
            ps.setLong(6, order.finalAmount());
            ps.setString(7, order.note());
            ps.setTimestamp(8, Timestamp.valueOf(order.createdAt()));
            ps.setTimestamp(9, Timestamp.valueOf(order.createdAt()));
        });
    }

    private Map<String, Long> loadSeedOrderIds() {
        return jdbc.query("SELECT id, note FROM orders WHERE note LIKE 'seed-order-%'",
                rs -> {
                    Map<String, Long> result = new HashMap<>();
                    while (rs.next()) result.put(rs.getString("note"), rs.getLong("id"));
                    return result;
                });
    }

    private List<OrderItemSeed> buildOrderItems(List<OrderSeed> orders, Map<String, Long> orderIds, List<VariantRef> variants) {
        List<OrderItemSeed> items = new ArrayList<>(orders.size() * 2);
        for (OrderSeed order : orders) {
            List<ItemDraft> drafts = itemDrafts(order.index(), variants);
            for (int i = 0; i < drafts.size(); i++) {
                ItemDraft draft = drafts.get(i);
                VariantRef variant = draft.variant();
                int quantity = draft.quantity();
                long subtotal = variant.price() * quantity;
                items.add(new OrderItemSeed(
                        orderIds.get(order.note()),
                        variant.id(),
                        "Product Item " + i,
                        variant.sku(),
                        quantity,
                        variant.price(),
                        subtotal
                ));
            }
        }
        return items;
    }

    private List<ItemDraft> itemDrafts(int orderIndex, List<VariantRef> variants) {
        Random itemRandom = new Random(10_000L + orderIndex);
        int itemCount = 1 + itemRandom.nextInt(3);
        List<ItemDraft> items = new ArrayList<>(itemCount);
        for (int i = 0; i < itemCount; i++) {
            items.add(new ItemDraft(
                    variants.get(itemRandom.nextInt(variants.size())),
                    1 + itemRandom.nextInt(2)
            ));
        }
        return items;
    }

    private void insertOrderItems(List<OrderItemSeed> items) {
        jdbc.batchUpdate("""
                INSERT INTO order_items (order_id, variant_id, name, sku, quantity, unit_price, subtotal)
                VALUES (?, ?, ?, ?, ?, ?, ?)
                """, items, BATCH_SIZE, (ps, item) -> {
            ps.setLong(1, item.orderId());
            ps.setLong(2, item.variantId());
            ps.setString(3, item.name());
            ps.setString(4, item.sku());
            ps.setInt(5, item.quantity());
            ps.setLong(6, item.unitPrice());
            ps.setLong(7, item.subtotal());
        });
    }

    private String addressJson() {
        return "{" +
                "\"name\":\"Customer " + random.nextInt(1000) + "\"," +
                "\"phone\":\"0" + (900000000 + random.nextInt(100000000)) + "\"," +
                "\"address\":\"" + random.nextInt(500) + " Main Street\"," +
                "\"ward\":\"Ward " + (1 + random.nextInt(20)) + "\"," +
                "\"district\":\"District " + (1 + random.nextInt(12)) + "\"," +
                "\"city\":\"Ho Chi Minh\"," +
                "\"country\":\"Vietnam\"" +
                "}";
    }

    private LocalDateTime randomDate() {
        return LocalDateTime.now().minusDays(random.nextInt(180));
    }

    private record VariantRef(Long id, String sku, String name, long price) {}
    private record ItemDraft(VariantRef variant, int quantity) {}

    private record OrderSeed(
            int index,
            Long userId,
            String shippingAddressJson,
            String status,
            long totalAmount,
            long shippingFee,
            long finalAmount,
            String note,
            LocalDateTime createdAt
    ) {}

    private record OrderItemSeed(
            Long orderId,
            Long variantId,
            String name,
            String sku,
            int quantity,
            long unitPrice,
            long subtotal
    ) {}
}
