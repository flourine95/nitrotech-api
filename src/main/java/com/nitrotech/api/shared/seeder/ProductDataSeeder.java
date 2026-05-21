package com.nitrotech.api.shared.seeder;

import com.nitrotech.api.infrastructure.persistence.entity.*;
import com.nitrotech.api.infrastructure.persistence.repository.BrandJpaRepository;
import com.nitrotech.api.infrastructure.persistence.repository.CategoryJpaRepository;
import com.nitrotech.api.infrastructure.persistence.repository.InventoryJpaRepository;
import com.nitrotech.api.infrastructure.persistence.repository.ProductJpaRepository;
import com.nitrotech.api.infrastructure.persistence.repository.ProductVariantJpaRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.stream.Collectors;

@Slf4j
@Component
@Profile("dev")
@Order(3)
@RequiredArgsConstructor
public class ProductDataSeeder implements CommandLineRunner {

    private static final int TOTAL_PRODUCTS = 1000;
    private static final int CHUNK_SIZE = 100;

    private final ProductJpaRepository productJpa;
    private final ProductVariantJpaRepository variantJpa;
    private final InventoryJpaRepository inventoryJpa;
    private final CategoryJpaRepository categoryJpa;
    private final BrandJpaRepository brandJpa;

    private final Random random = new Random(42);

    private static final Map<String, String> CATEGORY_NAMES = Map.ofEntries(
        Map.entry("cpu-intel", "CPU Intel"),
        Map.entry("cpu-amd", "CPU AMD"),
        Map.entry("vga-nvidia", "Card Màn Hình NVIDIA"),
        Map.entry("vga-amd", "Card Màn Hình AMD"),
        Map.entry("ram-ddr4", "RAM DDR4"),
        Map.entry("ram-ddr5", "RAM DDR5"),
        Map.entry("ssd-o-cung-the-ran", "SSD"),
        Map.entry("ban-phim-co", "Bàn Phím Cơ"),
        Map.entry("ban-phim-mang", "Bàn Phím Màng"),
        Map.entry("chuot-may-tinh", "Chuột"),
        Map.entry("tai-nghe", "Tai Nghe"),
        Map.entry("man-hinh-gaming", "Màn Hình Gaming"),
        Map.entry("man-hinh-van-phong", "Màn Hình Văn Phòng"),
        Map.entry("laptop-gaming", "Laptop Gaming"),
        Map.entry("laptop-van-phong", "Laptop Văn Phòng")
    );

    private static final String[] PRODUCT_ADJECTIVES = {
        "Cao Cấp", "Chuyên Nghiệp", "Siêu Mạnh", "Hiệu Năng Cao", "Tối Ưu",
        "Đỉnh Cao", "Thế Hệ Mới", "Nâng Cấp", "Tiên Tiến", "Mạnh Mẽ"
    };

    private static final String[] COLORS = {
        "Đen", "Trắng", "Xám", "Bạc", "Đỏ", "Xanh Navy", "Xanh Lá", "Vàng"
    };

    private static final String[] SIZES = { "Mini", "Standard", "XL", "Pro" };

    private static final Map<String, long[]> PRICE_RANGES = Map.ofEntries(
        Map.entry("cpu", new long[]{3_000_000, 20_000_000}),
        Map.entry("vga", new long[]{5_000_000, 50_000_000}),
        Map.entry("ram", new long[]{500_000, 8_000_000}),
        Map.entry("ssd", new long[]{500_000, 10_000_000}),
        Map.entry("ban-phim", new long[]{500_000, 5_000_000}),
        Map.entry("chuot", new long[]{300_000, 4_000_000}),
        Map.entry("tai-nghe", new long[]{500_000, 6_000_000}),
        Map.entry("man-hinh", new long[]{3_000_000, 30_000_000}),
        Map.entry("laptop-gaming", new long[]{20_000_000, 90_000_000}),
        Map.entry("laptop-van-phong", new long[]{10_000_000, 50_000_000})
    );

    @Override
    public void run(String... args) {
        if (productJpa.count() > 0) {
            log.info("Products already exist, skipping seed");
            return;
        }

        log.info("Seeding {} products...", TOTAL_PRODUCTS);

        Map<String, Long> categoryMap = categoryJpa.findAll().stream()
                .collect(Collectors.toMap(CategoryEntity::getSlug, CategoryEntity::getId));
        Map<String, Long> brandMap = brandJpa.findAll().stream()
                .collect(Collectors.toMap(BrandEntity::getSlug, BrandEntity::getId));

        List<String> availableCategories = CATEGORY_NAMES.keySet().stream()
                .filter(categoryMap::containsKey)
                .toList();

        List<String> availableBrands = new ArrayList<>(brandMap.keySet());

        if (availableCategories.isEmpty()) {
            log.warn("No categories found, skipping seed");
            return;
        }

        for (int chunk = 0; chunk < TOTAL_PRODUCTS; chunk += CHUNK_SIZE) {
            int batchSize = Math.min(CHUNK_SIZE, TOTAL_PRODUCTS - chunk);
            seedChunk(chunk, batchSize, categoryMap, availableCategories, brandMap, availableBrands);
        }

        log.info("Seeding completed");
    }

    @Transactional
    public void seedChunk(int offset, int count,
                          Map<String, Long> categoryMap, List<String> categorySlugPool,
                          Map<String, Long> brandMap, List<String> brandSlugPool) {

        List<ProductEntity> products = new ArrayList<>(count);
        List<ProductVariantEntity> variants = new ArrayList<>(count * 5);
        List<InventoryEntity> inventories = new ArrayList<>(count * 5);

        for (int i = 0; i < count; i++) {
            int globalIndex = offset + i;
            String categorySlug = categorySlugPool.get(globalIndex % categorySlugPool.size());
            String brandSlug = brandSlugPool.get(random.nextInt(brandSlugPool.size()));

            String name = generateName(globalIndex, categorySlug);
            String slug = generateSlug(name, globalIndex);

            ProductEntity p = new ProductEntity();
            p.setName(name);
            p.setSlug(slug);
            p.setDescription(generateDescription(name, categorySlug));
            p.setThumbnail(null);
            p.setCategoryId(categoryMap.get(categorySlug));
            p.setBrandId(brandMap.get(brandSlug));
            p.setActive(true);
            products.add(p);
        }

        List<ProductEntity> savedProducts = productJpa.saveAll(products);

        for (int i = 0; i < savedProducts.size(); i++) {
            ProductEntity saved = savedProducts.get(i);
            int globalIndex = offset + i;
            String categorySlug = categorySlugPool.get(globalIndex % categorySlugPool.size());

            int variantCount = 3 + random.nextInt(3);
            long[] priceRange = getPriceRange(categorySlug);

            for (int v = 0; v < variantCount; v++) {
                String color = COLORS[random.nextInt(COLORS.length)];
                String size = SIZES[v % SIZES.length];
                long price = priceRange[0] + (long) (random.nextDouble() * (priceRange[1] - priceRange[0]));
                price = (price / 10_000) * 10_000;

                ProductVariantEntity variant = new ProductVariantEntity();
                variant.setProductId(saved.getId());
                variant.setSku("SKU-" + globalIndex + "-V" + v);
                variant.setName(size + " / " + color);
                variant.setPrice(BigDecimal.valueOf(price));
                variant.setAttributes(Map.of("size", size, "color", color));
                variant.setActive(true);
                variants.add(variant);
            }
        }

        List<ProductVariantEntity> savedVariants = variantJpa.saveAll(variants);

        for (ProductVariantEntity v : savedVariants) {
            InventoryEntity inv = new InventoryEntity();
            inv.setVariantId(v.getId());
            inv.setQuantity(10 + random.nextInt(91));
            inv.setLowStockThreshold(5);
            inventories.add(inv);
        }

        inventoryJpa.saveAll(inventories);
    }

    private String generateName(int index, String categorySlug) {
        String categoryName = CATEGORY_NAMES.getOrDefault(categorySlug, "Sản Phẩm");
        String adjective = PRODUCT_ADJECTIVES[index % PRODUCT_ADJECTIVES.length];
        String series = String.format("%04d", index + 1);
        return categoryName + " " + adjective + " " + series;
    }

    private String generateSlug(String name, int index) {
        return name.toLowerCase()
                .replaceAll("[àáạảãâầấậẩẫăằắặẳẵ]", "a")
                .replaceAll("[èéẹẻẽêềếệểễ]", "e")
                .replaceAll("[ìíịỉĩ]", "i")
                .replaceAll("[òóọỏõôồốộổỗơờớợởỡ]", "o")
                .replaceAll("[ùúụủũưừứựửữ]", "u")
                .replaceAll("[ỳýỵỷỹ]", "y")
                .replaceAll("[đ]", "d")
                .replaceAll("[^a-z0-9\\s]", "")
                .replaceAll("\\s+", "-")
                + "-" + index;
    }

    private String generateDescription(String name, String categorySlug) {
        String categoryName = CATEGORY_NAMES.getOrDefault(categorySlug, "sản phẩm");
        return name + " - " + categoryName + " chất lượng cao, hiệu năng ổn định. Bảo hành chính hãng 12 tháng.";
    }

    private long[] getPriceRange(String categorySlug) {
        for (Map.Entry<String, long[]> entry : PRICE_RANGES.entrySet()) {
            if (categorySlug.startsWith(entry.getKey())) {
                return entry.getValue();
            }
        }
        return new long[]{500_000, 5_000_000};
    }
}
