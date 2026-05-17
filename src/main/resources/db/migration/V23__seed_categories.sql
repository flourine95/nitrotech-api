-- =====================================================
-- V23: Seed Categories for Electronics E-commerce
-- =====================================================

-- =====================================================
-- LEVEL 1: Main Categories (Root)
-- =====================================================

INSERT INTO categories (name, slug, description, parent_id, active, sort_order, created_at, updated_at) VALUES
('Laptop & Máy tính xách tay', 'laptop-may-tinh-xach-tay', 'Laptop, Ultrabook, Gaming laptop, Workstation', NULL, true, 0, NOW(), NOW()),
('PC & Máy tính để bàn', 'pc-may-tinh-de-ban', 'PC đồng bộ, PC gaming, PC văn phòng, Workstation', NULL, true, 1, NOW(), NOW()),
('Linh kiện máy tính', 'linh-kien-may-tinh', 'CPU, RAM, Mainboard, VGA, SSD, HDD và các linh kiện khác', NULL, true, 2, NOW(), NOW()),
('Thiết bị ngoại vi', 'thiet-bi-ngoai-vi', 'Bàn phím, chuột, tai nghe, webcam, loa', NULL, true, 3, NOW(), NOW()),
('Màn hình máy tính', 'man-hinh-may-tinh', 'Màn hình gaming, văn phòng, đồ họa, 4K', NULL, true, 4, NOW(), NOW()),
('Thiết bị mạng', 'thiet-bi-mang', 'Router, Switch, Access Point, Card mạng', NULL, true, 5, NOW(), NOW()),
('Phụ kiện & Gear', 'phu-kien-gear', 'Balo, túi chống sốc, đế tản nhiệt, hub USB', NULL, true, 6, NOW(), NOW());

-- =====================================================
-- LEVEL 2: Laptop Sub-categories
-- =====================================================

INSERT INTO categories (name, slug, description, parent_id, active, sort_order, created_at, updated_at) VALUES
('Laptop Gaming', 'laptop-gaming', 'Laptop chơi game hiệu năng cao, RTX, AMD Radeon', 1, true, 0, NOW(), NOW()),
('Laptop Văn phòng', 'laptop-van-phong', 'Laptop học tập, làm việc, mỏng nhẹ', 1, true, 1, NOW(), NOW()),
('Laptop Đồ họa - Sáng tạo', 'laptop-do-hoa-sang-tao', 'Laptop cho designer, video editor, 3D rendering', 1, true, 2, NOW(), NOW()),
('Laptop Doanh nghiệp', 'laptop-doanh-nghiep', 'ThinkPad, Latitude, ProBook, EliteBook', 1, true, 3, NOW(), NOW());

-- =====================================================
-- LEVEL 2: PC Sub-categories
-- =====================================================

INSERT INTO categories (name, slug, description, parent_id, active, sort_order, created_at, updated_at) VALUES
('PC Gaming', 'pc-gaming', 'PC chơi game cao cấp, RGB, tản nhiệt nước', 2, true, 0, NOW(), NOW()),
('PC Văn phòng', 'pc-van-phong', 'PC làm việc, học tập, giá rẻ', 2, true, 1, NOW(), NOW()),
('Workstation', 'workstation', 'PC chuyên dụng render, AI, server', 2, true, 2, NOW(), NOW());

-- =====================================================
-- LEVEL 2: Components Sub-categories
-- =====================================================

INSERT INTO categories (name, slug, description, parent_id, active, sort_order, created_at, updated_at) VALUES
('CPU - Bộ vi xử lý', 'cpu-bo-vi-xu-ly', 'Intel Core, AMD Ryzen, Threadripper', 3, true, 0, NOW(), NOW()),
('Mainboard - Bo mạch chủ', 'mainboard-bo-mach-chu', 'Intel, AMD, ATX, mATX, Mini-ITX', 3, true, 1, NOW(), NOW()),
('RAM - Bộ nhớ', 'ram-bo-nho', 'DDR4, DDR5, Desktop, Laptop', 3, true, 2, NOW(), NOW()),
('VGA - Card màn hình', 'vga-card-man-hinh', 'NVIDIA GeForce, AMD Radeon', 3, true, 3, NOW(), NOW()),
('SSD - Ổ cứng thể rắn', 'ssd-o-cung-the-ran', 'NVMe, SATA, M.2, PCIe Gen4', 3, true, 4, NOW(), NOW()),
('HDD - Ổ cứng', 'hdd-o-cung', 'Desktop, NAS, Enterprise', 3, true, 5, NOW(), NOW()),
('PSU - Nguồn máy tính', 'psu-nguon-may-tinh', '80 Plus, Modular, ATX, SFX', 3, true, 6, NOW(), NOW()),
('Case - Vỏ máy tính', 'case-vo-may-tinh', 'ATX, mATX, Mini-ITX, Full Tower', 3, true, 7, NOW(), NOW()),
('Tản nhiệt', 'tan-nhiet', 'Tản khí, tản nước AIO, custom loop', 3, true, 8, NOW(), NOW());

-- =====================================================
-- LEVEL 2: Peripherals Sub-categories
-- =====================================================

INSERT INTO categories (name, slug, description, parent_id, active, sort_order, created_at, updated_at) VALUES
('Bàn phím', 'ban-phim', 'Cơ, màng, gaming, văn phòng', 4, true, 0, NOW(), NOW()),
('Chuột máy tính', 'chuot-may-tinh', 'Gaming, văn phòng, không dây', 4, true, 1, NOW(), NOW()),
('Tai nghe', 'tai-nghe', 'Gaming, âm nhạc, có mic, không dây', 4, true, 2, NOW(), NOW()),
('Loa máy tính', 'loa-may-tinh', '2.0, 2.1, 5.1, soundbar', 4, true, 3, NOW(), NOW()),
('Webcam', 'webcam', 'HD, Full HD, 4K, streaming', 4, true, 4, NOW(), NOW());

-- =====================================================
-- LEVEL 2: Monitor Sub-categories
-- =====================================================

INSERT INTO categories (name, slug, description, parent_id, active, sort_order, created_at, updated_at) VALUES
('Màn hình Gaming', 'man-hinh-gaming', '144Hz, 240Hz, G-Sync, FreeSync', 5, true, 0, NOW(), NOW()),
('Màn hình Văn phòng', 'man-hinh-van-phong', '24", 27", Full HD, IPS', 5, true, 1, NOW(), NOW()),
('Màn hình Đồ họa', 'man-hinh-do-hoa', '4K, 5K, Adobe RGB, DCI-P3', 5, true, 2, NOW(), NOW());

-- =====================================================
-- LEVEL 3: Deep Sub-categories
-- =====================================================

-- CPU brands (parent: CPU - Bộ vi xử lý)
INSERT INTO categories (name, slug, description, parent_id, active, sort_order, created_at, updated_at)
SELECT 'CPU Intel', 'cpu-intel', 'Core i3, i5, i7, i9, Xeon', id, true, 0, NOW(), NOW()
FROM categories WHERE slug = 'cpu-bo-vi-xu-ly';

INSERT INTO categories (name, slug, description, parent_id, active, sort_order, created_at, updated_at)
SELECT 'CPU AMD', 'cpu-amd', 'Ryzen 3, 5, 7, 9, Threadripper', id, true, 1, NOW(), NOW()
FROM categories WHERE slug = 'cpu-bo-vi-xu-ly';

-- VGA brands (parent: VGA - Card màn hình)
INSERT INTO categories (name, slug, description, parent_id, active, sort_order, created_at, updated_at)
SELECT 'VGA NVIDIA', 'vga-nvidia', 'GeForce RTX 4090, 4080, 4070, 4060', id, true, 0, NOW(), NOW()
FROM categories WHERE slug = 'vga-card-man-hinh';

INSERT INTO categories (name, slug, description, parent_id, active, sort_order, created_at, updated_at)
SELECT 'VGA AMD', 'vga-amd', 'Radeon RX 7900, 7800, 7700, 7600', id, true, 1, NOW(), NOW()
FROM categories WHERE slug = 'vga-card-man-hinh';

-- RAM types (parent: RAM - Bộ nhớ)
INSERT INTO categories (name, slug, description, parent_id, active, sort_order, created_at, updated_at)
SELECT 'RAM DDR4', 'ram-ddr4', 'Desktop DDR4, 8GB, 16GB, 32GB', id, true, 0, NOW(), NOW()
FROM categories WHERE slug = 'ram-bo-nho';

INSERT INTO categories (name, slug, description, parent_id, active, sort_order, created_at, updated_at)
SELECT 'RAM DDR5', 'ram-ddr5', 'Desktop DDR5, 16GB, 32GB, 64GB', id, true, 1, NOW(), NOW()
FROM categories WHERE slug = 'ram-bo-nho';

-- Keyboard types (parent: Bàn phím)
INSERT INTO categories (name, slug, description, parent_id, active, sort_order, created_at, updated_at)
SELECT 'Bàn phím cơ', 'ban-phim-co', 'Cherry MX, Gateron, Kailh', id, true, 0, NOW(), NOW()
FROM categories WHERE slug = 'ban-phim';

INSERT INTO categories (name, slug, description, parent_id, active, sort_order, created_at, updated_at)
SELECT 'Bàn phím màng', 'ban-phim-mang', 'Giá rẻ, văn phòng, gaming', id, true, 1, NOW(), NOW()
FROM categories WHERE slug = 'ban-phim';

-- =====================================================
-- Summary
-- =====================================================
-- Total categories: 45
-- Level 1 (Root): 7 categories
-- Level 2: 30 categories
-- Level 3: 8 categories
-- All with proper sortOrder and hierarchy
-- =====================================================
