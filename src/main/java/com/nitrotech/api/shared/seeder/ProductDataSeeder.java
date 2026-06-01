package com.nitrotech.api.shared.seeder;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.core.annotation.Order;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;

@Slf4j
@Component
@Profile("dev")
@Order(6)
@RequiredArgsConstructor
public class ProductDataSeeder implements CommandLineRunner {

    private static final int TOTAL_PRODUCTS = 100;
    private static final int BATCH_SIZE = 100;

    private final JdbcTemplate jdbc;
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

    private static final Map<String, List<String>> CATEGORY_BRANDS = Map.ofEntries(
            Map.entry("laptop-gaming", List.of("asus", "msi", "dell", "hp", "lenovo", "acer", "gigabyte")),
            Map.entry("laptop-van-phong", List.of("apple", "dell", "hp", "lenovo", "asus", "acer")),
            Map.entry("cpu-intel", List.of("intel")),
            Map.entry("cpu-amd", List.of("amd")),
            Map.entry("vga-nvidia", List.of("nvidia", "asus-rog", "msi-gaming", "gigabyte-aorus", "zotac", "palit", "colorful", "inno3d")),
            Map.entry("vga-amd", List.of("amd", "asus-rog", "msi-gaming", "gigabyte-aorus")),
            Map.entry("ram-ddr4", List.of("corsair", "gskill", "kingston", "crucial", "teamgroup", "adata")),
            Map.entry("ram-ddr5", List.of("corsair", "gskill", "kingston", "crucial", "teamgroup", "adata")),
            Map.entry("ssd-o-cung-the-ran", List.of("samsung", "western-digital", "seagate", "crucial-mx", "sk-hynix")),
            Map.entry("ban-phim-co", List.of("logitech", "razer", "steelseries", "hyperx", "corsair-gaming", "keychron", "akko", "ducky")),
            Map.entry("ban-phim-mang", List.of("logitech", "razer", "steelseries", "hyperx", "corsair-gaming")),
            Map.entry("chuot-may-tinh", List.of("logitech", "razer", "steelseries", "corsair-gaming")),
            Map.entry("tai-nghe", List.of("logitech", "razer", "steelseries", "hyperx", "corsair-gaming")),
            Map.entry("man-hinh-gaming", List.of("lg", "benq", "viewsonic", "aoc")),
            Map.entry("man-hinh-van-phong", List.of("lg", "benq", "viewsonic", "aoc"))
    );

    private static final Map<String, List<String>> THUMBNAILS = Map.ofEntries(
            Map.entry("laptop-gaming", List.of(
                    "https://res.cloudinary.com/dk3gud5kq/image/upload/v1780216838/euw6hd9vuwhrpl6einvt.webp",
                    "https://res.cloudinary.com/dk3gud5kq/image/upload/v1780216838/ewpohkawwq7irpkqrxp2.jpg",
                    "https://res.cloudinary.com/dk3gud5kq/image/upload/v1780216838/tnbcsubcsvvy3leo7rwv.jpg",
                    "https://res.cloudinary.com/dk3gud5kq/image/upload/v1780216838/trphcqyqxn7khai1hbwk.jpg",
                    "https://res.cloudinary.com/dk3gud5kq/image/upload/v1780216837/nlbzzkxqkz9xpxshcxg5.webp",
                    "https://res.cloudinary.com/dk3gud5kq/image/upload/v1780216837/uudzpy6zkhjku2muw3wg.jpg"
            )),
            Map.entry("laptop-van-phong", List.of(
                    "https://res.cloudinary.com/dk3gud5kq/image/upload/v1780216837/mg32586pzeh3brcwgefz.jpg",
                    "https://res.cloudinary.com/dk3gud5kq/image/upload/v1780216837/tf60v5ihjhk8fmgratci.jpg",
                    "https://res.cloudinary.com/dk3gud5kq/image/upload/v1780216837/vcpw7ohedsfv4j7qfitf.jpg",
                    "https://res.cloudinary.com/dk3gud5kq/image/upload/v1780216836/fkoafbwx943pj2bkk0q4.jpg",
                    "https://res.cloudinary.com/dk3gud5kq/image/upload/v1780216836/arebhrvp7ra2tjk8u5rb.jpg"
            )),
            Map.entry("vga-nvidia", List.of(
                    "https://res.cloudinary.com/dk3gud5kq/image/upload/v1780216836/otkmqid6mjbl3yxfth8t.jpg",
                    "https://res.cloudinary.com/dk3gud5kq/image/upload/v1780216836/ghumiztlbsnznd5a8gej.jpg",
                    "https://res.cloudinary.com/dk3gud5kq/image/upload/v1780216836/hxrkxfmctzes7wjf8ryi.jpg",
                    "https://res.cloudinary.com/dk3gud5kq/image/upload/v1780216835/jy4rdlla9k3lthnuqwxo.jpg"
            )),
            Map.entry("vga-amd", List.of(
                    "https://res.cloudinary.com/dk3gud5kq/image/upload/v1780216837/tewzqfgk0ilxm0ng8lc5.jpg",
                    "https://res.cloudinary.com/dk3gud5kq/image/upload/v1780216835/jy4rdlla9k3lthnuqwxo.jpg",
                    "https://res.cloudinary.com/dk3gud5kq/image/upload/v1780216835/uwomkafwuk4sspn8dx6h.jpg"
            )),
            Map.entry("ssd-o-cung-the-ran", List.of(
                    "https://res.cloudinary.com/dk3gud5kq/image/upload/v1780216837/myiajmgzta22qdnmhd2o.jpg",
                    "https://res.cloudinary.com/dk3gud5kq/image/upload/v1780216836/uooalzicwb2wstvsixgk.avif",
                    "https://res.cloudinary.com/dk3gud5kq/image/upload/v1780216836/tuns1wogrrujlrl2ylnl.jpg",
                    "https://res.cloudinary.com/dk3gud5kq/image/upload/v1780216836/tz0iracsi6qtvvva6lhd.jpg"
            )),
            Map.entry("man-hinh-gaming", List.of(
                    "https://res.cloudinary.com/dk3gud5kq/image/upload/v1780216835/zpecuqqs3gcmfyeimupm.webp",
                    "https://res.cloudinary.com/dk3gud5kq/image/upload/v1780216835/zcfzi2yhl4suethdfgjd.jpg",
                    "https://res.cloudinary.com/dk3gud5kq/image/upload/v1780216835/pm53o5lvpmwnzebh3whc.webp"
            )),
            Map.entry("man-hinh-van-phong", List.of(
                    "https://res.cloudinary.com/dk3gud5kq/image/upload/v1780216835/tmfxishkauft6d3dfqur.webp",
                    "https://res.cloudinary.com/dk3gud5kq/image/upload/v1780216835/sxtdsceg0b74zqtfygyd.avif"
            )),
            Map.entry("ban-phim-co", List.of(
                    "https://res.cloudinary.com/dk3gud5kq/image/upload/v1780216834/pm53o5lvpmwnzebh3whc.webp",
                    "https://res.cloudinary.com/dk3gud5kq/image/upload/v1780216834/pwxxyrf83oyzdc0w0et0.jpg"
            )),
            Map.entry("ban-phim-mang", List.of(
                    "https://res.cloudinary.com/dk3gud5kq/image/upload/v1780216834/yvmtz7zvbjlen2qxv4aq.webp",
                    "https://res.cloudinary.com/dk3gud5kq/image/upload/v1780216834/swdneogboawl2nthsrbj.jpg"
            )),
            Map.entry("cpu-intel", List.of(
                    "https://res.cloudinary.com/dk3gud5kq/image/upload/v1780216838/tuxhh2d0qesmy6wagnxy.jpg",
                    "https://res.cloudinary.com/dk3gud5kq/image/upload/v1780216835/t4aqiojybqwgoywczyw3.jpg",
                    "https://res.cloudinary.com/dk3gud5kq/image/upload/v1780216833/ziihpcyebd86mnatcklh.avif"
            )),
            Map.entry("cpu-amd", List.of(
                    "https://res.cloudinary.com/dk3gud5kq/image/upload/v1780216834/uycbfemer8m1npbg1eu8.jpg",
                    "https://res.cloudinary.com/dk3gud5kq/image/upload/v1780216834/tqur22jz1yreev8vrh7c.webp"
            )),
            Map.entry("ram-ddr4", List.of(
                    "https://res.cloudinary.com/dk3gud5kq/image/upload/v1780216838/tdeda8ecxw0nxpythkaf.jpg",
                    "https://res.cloudinary.com/dk3gud5kq/image/upload/v1780216838/kywcjf9ocuijkiwdifn5.jpg",
                    "https://res.cloudinary.com/dk3gud5kq/image/upload/v1780216835/jwdg98ovl0p6raj3nat4.jpg"
            )),
            Map.entry("ram-ddr5", List.of(
                    "https://res.cloudinary.com/dk3gud5kq/image/upload/v1780216838/fynlncziaek6qgiq5lk8.jpg",
                    "https://res.cloudinary.com/dk3gud5kq/image/upload/v1780216837/hks9jatn5orjhyldyrn4.jpg"
            ))
    );

    @Override
    @Transactional
    public void run(String... args) {
        Long existing = jdbc.queryForObject("SELECT COUNT(*) FROM products", Long.class);
        if (existing != null && existing > 0) {
            log.info("Products already exist, skipping seed");
            return;
        }

        Map<String, Long> categoryMap = loadIdMap("categories");
        Map<String, Long> brandMap = loadIdMap("brands");
        List<String> categorySlugs = CATEGORY_NAMES.keySet().stream()
                .filter(categoryMap::containsKey)
                .toList();

        if (categorySlugs.isEmpty() || brandMap.isEmpty()) {
            log.warn("Missing categories or brands, skipping product seed");
            return;
        }

        log.info("Seeding {} products with JDBC batch...", TOTAL_PRODUCTS);

        List<ProductSeed> products = buildProducts(categoryMap, brandMap, categorySlugs);
        insertProducts(products);

        Map<String, Long> productIds = loadProductIds();
        List<VariantSeed> variants = buildVariants(products, productIds);
        insertProductImages(products, productIds);
        insertVariants(variants);

        Map<String, Long> variantIds = loadVariantIds();
        insertInventories(variants, variantIds);

        log.info("JDBC product seed completed: {} products, {} variants", products.size(), variants.size());
    }

    private Map<String, Long> loadIdMap(String table) {
        return jdbc.query("SELECT id, slug FROM " + table + " WHERE deleted_at IS NULL",
                rs -> {
                    Map<String, Long> result = new HashMap<>();
                    while (rs.next()) result.put(rs.getString("slug"), rs.getLong("id"));
                    return result;
                });
    }

    private List<ProductSeed> buildProducts(
            Map<String, Long> categoryMap,
            Map<String, Long> brandMap,
            List<String> categorySlugs
    ) {
        List<ProductSeed> products = new ArrayList<>(TOTAL_PRODUCTS);
        Map<String, Integer> categoryCounts = new HashMap<>();
        for (int i = 0; i < TOTAL_PRODUCTS; i++) {
            String categorySlug = categorySlugs.get(i % categorySlugs.size());
            int categoryIndex = categoryCounts.merge(categorySlug, 1, Integer::sum) - 1;
            String brandSlug = brandSlugFor(categorySlug, categoryIndex, brandMap);
            String name = generateName(i, categorySlug);
            String slug = generateSlug(name, i);
            String thumbnail = thumbnailFor(categorySlug, i);

            products.add(new ProductSeed(
                    i,
                    categorySlug,
                    categoryMap.get(categorySlug),
                    brandMap.get(brandSlug),
                    name,
                    slug,
                    generateDescription(name, categorySlug),
                    thumbnail,
                    specsFor(categorySlug, i)
            ));
        }
        return products;
    }

    private String brandSlugFor(String categorySlug, int index, Map<String, Long> brandMap) {
        List<String> brands = CATEGORY_BRANDS.getOrDefault(categorySlug, List.of()).stream()
                .filter(brandMap::containsKey)
                .toList();
        if (!brands.isEmpty()) return brands.get(index % brands.size());
        return brandMap.keySet().stream().sorted().findFirst().orElseThrow();
    }

    private void insertProducts(List<ProductSeed> products) {
        jdbc.batchUpdate("""
                INSERT INTO products (category_id, brand_id, name, slug, description, thumbnail, specs, active)
                VALUES (?, ?, ?, ?, ?, ?, ?::jsonb, true)
                """, products, BATCH_SIZE, (ps, p) -> {
            ps.setLong(1, p.categoryId());
            ps.setLong(2, p.brandId());
            ps.setString(3, p.name());
            ps.setString(4, p.slug());
            ps.setString(5, p.description());
            ps.setString(6, p.thumbnail());
            ps.setString(7, p.specsJson());
        });
    }

    private Map<String, Long> loadProductIds() {
        return jdbc.query("SELECT id, slug FROM products WHERE slug LIKE 'seed-%'",
                rs -> {
                    Map<String, Long> result = new HashMap<>();
                    while (rs.next()) result.put(rs.getString("slug"), rs.getLong("id"));
                    return result;
                });
    }

    private void insertProductImages(List<ProductSeed> products, Map<String, Long> productIds) {
        List<ProductImageSeed> images = new ArrayList<>();
        for (ProductSeed product : products) {
            List<String> thumbnails = THUMBNAILS.get(product.categorySlug());
            if (thumbnails == null || thumbnails.isEmpty()) continue;

            int imageCount = Math.min(4, thumbnails.size());
            for (int offset = 0; offset < imageCount; offset++) {
                String url = thumbnails.get((product.index() + offset) % thumbnails.size());
                images.add(new ProductImageSeed(productIds.get(product.slug()), url, offset));
            }
        }

        jdbc.batchUpdate("""
                INSERT INTO product_images (product_id, url, sort_order)
                VALUES (?, ?, ?)
                """, images, BATCH_SIZE, (ps, image) -> {
            ps.setLong(1, image.productId());
            ps.setString(2, image.url());
            ps.setInt(3, image.sortOrder());
        });
    }

    private List<VariantSeed> buildVariants(List<ProductSeed> products, Map<String, Long> productIds) {
        List<VariantSeed> variants = new ArrayList<>(products.size() * 4);
        for (ProductSeed product : products) {
            int variantCount = 3 + random.nextInt(3);
            long[] priceRange = getPriceRange(product.categorySlug());

            for (int v = 0; v < variantCount; v++) {
                String color = COLORS[random.nextInt(COLORS.length)];
                String size = SIZES[v % SIZES.length];
                long price = priceRange[0] + (long) (random.nextDouble() * (priceRange[1] - priceRange[0]));
                price = (price / 10_000) * 10_000;

                variants.add(new VariantSeed(
                        productIds.get(product.slug()),
                        "SEED-SKU-" + product.index() + "-V" + v,
                        size + " / " + color,
                        price,
                        "{\"size\":\"" + size + "\",\"color\":\"" + color + "\"}"
                ));
            }
        }
        return variants;
    }

    private void insertVariants(List<VariantSeed> variants) {
        jdbc.batchUpdate("""
                INSERT INTO product_variants (product_id, sku, name, price, attributes, active)
                VALUES (?, ?, ?, ?, ?::jsonb, true)
                """, variants, BATCH_SIZE, (ps, variant) -> {
            ps.setLong(1, variant.productId());
            ps.setString(2, variant.sku());
            ps.setString(3, variant.name());
            ps.setLong(4, variant.price());
            ps.setString(5, variant.attributesJson());
        });
    }

    private Map<String, Long> loadVariantIds() {
        return jdbc.query("SELECT id, sku FROM product_variants WHERE sku LIKE 'SEED-SKU-%'",
                rs -> {
                    Map<String, Long> result = new HashMap<>();
                    while (rs.next()) result.put(rs.getString("sku"), rs.getLong("id"));
                    return result;
                });
    }

    private void insertInventories(List<VariantSeed> variants, Map<String, Long> variantIds) {
        jdbc.batchUpdate("""
                INSERT INTO inventories (variant_id, quantity, low_stock_threshold)
                VALUES (?, ?, 5)
                """, variants, BATCH_SIZE, (ps, variant) -> {
            ps.setLong(1, variantIds.get(variant.sku()));
            ps.setInt(2, 10 + random.nextInt(91));
        });
    }

    private String generateName(int index, String categorySlug) {
        String categoryName = CATEGORY_NAMES.getOrDefault(categorySlug, "Sản Phẩm");
        String adjective = PRODUCT_ADJECTIVES[index % PRODUCT_ADJECTIVES.length];
        String series = String.format("%04d", index + 1);
        return categoryName + " " + adjective + " " + series;
    }

    private String thumbnailFor(String categorySlug, int index) {
        List<String> thumbnails = THUMBNAILS.get(categorySlug);
        if (thumbnails == null || thumbnails.isEmpty()) return null;
        return thumbnails.get(index % thumbnails.size());
    }

    private String generateSlug(String name, int index) {
        return "seed-" + name.toLowerCase()
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

    private String specsFor(String categorySlug, int index) {
        return switch (categorySlug) {
            case "laptop-gaming" -> json(Map.of(
                    "CPU", pick(index, "Intel Core i7-14650HX", "AMD Ryzen 9 8945HS", "Intel Core i9-14900HX"),
                    "GPU", pick(index, "NVIDIA GeForce RTX 4060", "NVIDIA GeForce RTX 4070", "NVIDIA GeForce RTX 4080"),
                    "RAM", pick(index, "16GB DDR5", "32GB DDR5"),
                    "Storage", pick(index, "1TB NVMe SSD", "2TB NVMe SSD"),
                    "Display", pick(index, "16 inch QHD 165Hz", "17.3 inch FHD 144Hz")
            ));
            case "laptop-van-phong" -> json(Map.of(
                    "CPU", pick(index, "Intel Core Ultra 5", "Intel Core i5-1340P", "AMD Ryzen 7 7730U"),
                    "RAM", pick(index, "8GB LPDDR5", "16GB LPDDR5"),
                    "Storage", pick(index, "512GB NVMe SSD", "1TB NVMe SSD"),
                    "Display", pick(index, "14 inch FHD IPS", "15.6 inch FHD IPS"),
                    "Weight", pick(index, "1.25kg", "1.45kg", "1.65kg")
            ));
            case "cpu-intel", "cpu-amd" -> json(Map.of(
                    "Cores", pick(index, "6 cores / 12 threads", "8 cores / 16 threads", "16 cores / 32 threads"),
                    "Max Boost", pick(index, "4.7GHz", "5.2GHz", "5.7GHz"),
                    "Socket", categorySlug.equals("cpu-intel") ? "LGA1700" : "AM5",
                    "TDP", pick(index, "65W", "105W", "125W")
            ));
            case "vga-nvidia", "vga-amd" -> json(Map.of(
                    "VRAM", pick(index, "8GB GDDR6", "12GB GDDR6X", "16GB GDDR6"),
                    "Interface", "PCIe 4.0 x16",
                    "Power", pick(index, "170W", "220W", "320W"),
                    "Outputs", "HDMI, DisplayPort"
            ));
            case "ram-ddr4", "ram-ddr5" -> json(Map.of(
                    "Capacity", pick(index, "16GB (2x8GB)", "32GB (2x16GB)", "64GB (2x32GB)"),
                    "Type", categorySlug.equals("ram-ddr5") ? "DDR5" : "DDR4",
                    "Speed", categorySlug.equals("ram-ddr5") ? pick(index, "5600MHz", "6000MHz", "6400MHz") : pick(index, "3200MHz", "3600MHz"),
                    "Latency", pick(index, "CL16", "CL30", "CL36")
            ));
            case "ssd-o-cung-the-ran" -> json(Map.of(
                    "Capacity", pick(index, "500GB", "1TB", "2TB"),
                    "Form Factor", "M.2 2280",
                    "Interface", pick(index, "PCIe Gen3 x4", "PCIe Gen4 x4"),
                    "Read Speed", pick(index, "3500MB/s", "5000MB/s", "7000MB/s")
            ));
            case "man-hinh-gaming", "man-hinh-van-phong" -> json(Map.of(
                    "Size", pick(index, "24 inch", "27 inch", "32 inch"),
                    "Resolution", pick(index, "1920x1080", "2560x1440", "3840x2160"),
                    "Panel", pick(index, "IPS", "VA", "OLED"),
                    "Refresh Rate", categorySlug.equals("man-hinh-gaming") ? pick(index, "144Hz", "165Hz", "240Hz") : pick(index, "75Hz", "100Hz")
            ));
            case "ban-phim-co", "ban-phim-mang" -> json(Map.of(
                    "Layout", pick(index, "Full-size", "TKL", "75%"),
                    "Switch", categorySlug.equals("ban-phim-co") ? pick(index, "Red switch", "Brown switch", "Blue switch") : "Membrane",
                    "Connection", pick(index, "USB-C", "Bluetooth", "2.4GHz Wireless"),
                    "Backlight", pick(index, "RGB", "White LED", "None")
            ));
            case "chuot-may-tinh" -> json(Map.of(
                    "Sensor", pick(index, "Optical 12K DPI", "Optical 20K DPI", "Laser 8K DPI"),
                    "Connection", pick(index, "USB", "Bluetooth", "2.4GHz Wireless"),
                    "Buttons", pick(index, "6 buttons", "8 buttons", "11 buttons"),
                    "Weight", pick(index, "59g", "80g", "95g")
            ));
            case "tai-nghe" -> json(Map.of(
                    "Driver", pick(index, "40mm", "50mm"),
                    "Connection", pick(index, "3.5mm", "USB", "Bluetooth"),
                    "Microphone", pick(index, "Detachable", "Flip-to-mute", "Built-in"),
                    "Surround", pick(index, "Stereo", "7.1 Virtual")
            ));
            default -> "{}";
        };
    }

    private String pick(int index, String... values) {
        return values[index % values.length];
    }

    private String json(Map<String, String> values) {
        return values.entrySet().stream()
                .map(e -> "\"" + escapeJson(e.getKey()) + "\":\"" + escapeJson(e.getValue()) + "\"")
                .reduce("{", (acc, item) -> acc.equals("{") ? acc + item : acc + "," + item) + "}";
    }

    private String escapeJson(String value) {
        return value.replace("\\", "\\\\").replace("\"", "\\\"");
    }

    private long[] getPriceRange(String categorySlug) {
        for (Map.Entry<String, long[]> entry : PRICE_RANGES.entrySet()) {
            if (categorySlug.startsWith(entry.getKey())) {
                return entry.getValue();
            }
        }
        return new long[]{500_000, 5_000_000};
    }

    private record ProductSeed(
            int index,
            String categorySlug,
            Long categoryId,
            Long brandId,
            String name,
            String slug,
            String description,
            String thumbnail,
            String specsJson
    ) {}

    private record ProductImageSeed(Long productId, String url, int sortOrder) {}

    private record VariantSeed(
            Long productId,
            String sku,
            String name,
            long price,
            String attributesJson
    ) {}
}
