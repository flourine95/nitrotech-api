package com.nitrotech.api.shared.seeder;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.core.annotation.Order;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Slf4j
@Component
@Profile("dev")
@Order(4)
@RequiredArgsConstructor
public class CategoryDataSeeder implements CommandLineRunner {

    private final JdbcTemplate jdbc;

    @Override
    @Transactional
    public void run(String... args) {
        Long existing = jdbc.queryForObject("SELECT COUNT(*) FROM categories", Long.class);
        if (existing != null && existing > 0) {
            log.info("Categories already exist, skipping seed");
            return;
        }

        List<CategorySeed> categories = List.of(
                new CategorySeed("Laptop & Máy tính xách tay", "laptop-may-tinh-xach-tay", "Laptop, Ultrabook, Gaming laptop, Workstation", null, 0),
                new CategorySeed("PC & Máy tính để bàn", "pc-may-tinh-de-ban", "PC đồng bộ, PC gaming, PC văn phòng, Workstation", null, 1),
                new CategorySeed("Linh kiện máy tính", "linh-kien-may-tinh", "CPU, RAM, Mainboard, VGA, SSD, HDD và các linh kiện khác", null, 2),
                new CategorySeed("Thiết bị ngoại vi", "thiet-bi-ngoai-vi", "Bàn phím, chuột, tai nghe, webcam, loa", null, 3),
                new CategorySeed("Màn hình máy tính", "man-hinh-may-tinh", "Màn hình gaming, văn phòng, đồ họa, 4K", null, 4),
                new CategorySeed("Thiết bị mạng", "thiet-bi-mang", "Router, Switch, Access Point, Card mạng", null, 5),
                new CategorySeed("Phụ kiện & Gear", "phu-kien-gear", "Balo, túi chống sốc, đế tản nhiệt, hub USB", null, 6),
                new CategorySeed("Laptop Gaming", "laptop-gaming", "Laptop chơi game hiệu năng cao, RTX, AMD Radeon", "laptop-may-tinh-xach-tay", 0),
                new CategorySeed("Laptop Văn phòng", "laptop-van-phong", "Laptop học tập, làm việc, mỏng nhẹ", "laptop-may-tinh-xach-tay", 1),
                new CategorySeed("Laptop Đồ họa - Sáng tạo", "laptop-do-hoa-sang-tao", "Laptop cho designer, video editor, 3D rendering", "laptop-may-tinh-xach-tay", 2),
                new CategorySeed("Laptop Doanh nghiệp", "laptop-doanh-nghiep", "ThinkPad, Latitude, ProBook, EliteBook", "laptop-may-tinh-xach-tay", 3),
                new CategorySeed("PC Gaming", "pc-gaming", "PC chơi game cao cấp, RGB, tản nhiệt nước", "pc-may-tinh-de-ban", 0),
                new CategorySeed("PC Văn phòng", "pc-van-phong", "PC làm việc, học tập, giá rẻ", "pc-may-tinh-de-ban", 1),
                new CategorySeed("Workstation", "workstation", "PC chuyên dụng render, AI, server", "pc-may-tinh-de-ban", 2),
                new CategorySeed("CPU - Bộ vi xử lý", "cpu-bo-vi-xu-ly", "Intel Core, AMD Ryzen, Threadripper", "linh-kien-may-tinh", 0),
                new CategorySeed("Mainboard - Bo mạch chủ", "mainboard-bo-mach-chu", "Intel, AMD, ATX, mATX, Mini-ITX", "linh-kien-may-tinh", 1),
                new CategorySeed("RAM - Bộ nhớ", "ram-bo-nho", "DDR4, DDR5, Desktop, Laptop", "linh-kien-may-tinh", 2),
                new CategorySeed("VGA - Card màn hình", "vga-card-man-hinh", "NVIDIA GeForce, AMD Radeon", "linh-kien-may-tinh", 3),
                new CategorySeed("SSD - Ổ cứng thể rắn", "ssd-o-cung-the-ran", "NVMe, SATA, M.2, PCIe Gen4", "linh-kien-may-tinh", 4),
                new CategorySeed("HDD - Ổ cứng", "hdd-o-cung", "Desktop, NAS, Enterprise", "linh-kien-may-tinh", 5),
                new CategorySeed("PSU - Nguồn máy tính", "psu-nguon-may-tinh", "80 Plus, Modular, ATX, SFX", "linh-kien-may-tinh", 6),
                new CategorySeed("Case - Vỏ máy tính", "case-vo-may-tinh", "ATX, mATX, Mini-ITX, Full Tower", "linh-kien-may-tinh", 7),
                new CategorySeed("Tản nhiệt", "tan-nhiet", "Tản khí, tản nước AIO, custom loop", "linh-kien-may-tinh", 8),
                new CategorySeed("Bàn phím", "ban-phim", "Cơ, màng, gaming, văn phòng", "thiet-bi-ngoai-vi", 0),
                new CategorySeed("Chuột máy tính", "chuot-may-tinh", "Gaming, văn phòng, không dây", "thiet-bi-ngoai-vi", 1),
                new CategorySeed("Tai nghe", "tai-nghe", "Gaming, âm nhạc, có mic, không dây", "thiet-bi-ngoai-vi", 2),
                new CategorySeed("Loa máy tính", "loa-may-tinh", "2.0, 2.1, 5.1, soundbar", "thiet-bi-ngoai-vi", 3),
                new CategorySeed("Webcam", "webcam", "HD, Full HD, 4K, streaming", "thiet-bi-ngoai-vi", 4),
                new CategorySeed("Màn hình Gaming", "man-hinh-gaming", "144Hz, 240Hz, G-Sync, FreeSync", "man-hinh-may-tinh", 0),
                new CategorySeed("Màn hình Văn phòng", "man-hinh-van-phong", "24\", 27\", Full HD, IPS", "man-hinh-may-tinh", 1),
                new CategorySeed("Màn hình Đồ họa", "man-hinh-do-hoa", "4K, 5K, Adobe RGB, DCI-P3", "man-hinh-may-tinh", 2),
                new CategorySeed("CPU Intel", "cpu-intel", "Core i3, i5, i7, i9, Xeon", "cpu-bo-vi-xu-ly", 0),
                new CategorySeed("CPU AMD", "cpu-amd", "Ryzen 3, 5, 7, 9, Threadripper", "cpu-bo-vi-xu-ly", 1),
                new CategorySeed("VGA NVIDIA", "vga-nvidia", "GeForce RTX 4090, 4080, 4070, 4060", "vga-card-man-hinh", 0),
                new CategorySeed("VGA AMD", "vga-amd", "Radeon RX 7900, 7800, 7700, 7600", "vga-card-man-hinh", 1),
                new CategorySeed("RAM DDR4", "ram-ddr4", "Desktop DDR4, 8GB, 16GB, 32GB", "ram-bo-nho", 0),
                new CategorySeed("RAM DDR5", "ram-ddr5", "Desktop DDR5, 16GB, 32GB, 64GB", "ram-bo-nho", 1),
                new CategorySeed("Bàn phím cơ", "ban-phim-co", "Cherry MX, Gateron, Kailh", "ban-phim", 0),
                new CategorySeed("Bàn phím màng", "ban-phim-mang", "Giá rẻ, văn phòng, gaming", "ban-phim", 1)
        );

        insertCategories(categories.subList(0, 7));
        insertCategories(categories.subList(7, 31));
        insertCategories(categories.subList(31, categories.size()));

        log.info("Seeded {} categories", categories.size());
    }

    private void insertCategories(List<CategorySeed> categories) {
        jdbc.batchUpdate("""
                INSERT INTO categories (name, slug, description, parent_id, active, sort_order)
                VALUES (?, ?, ?, (SELECT id FROM categories WHERE slug = ? AND deleted_at IS NULL), true, ?)
                """, categories, categories.size(), (ps, category) -> {
            ps.setString(1, category.name());
            ps.setString(2, category.slug());
            ps.setString(3, category.description());
            ps.setString(4, category.parentSlug());
            ps.setInt(5, category.sortOrder());
        });
    }

    private record CategorySeed(String name, String slug, String description, String parentSlug, int sortOrder) {}
}
