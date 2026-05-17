-- =====================================================
-- V24: Seed Brands for Electronics E-commerce
-- =====================================================

-- =====================================================
-- Laptop & PC Brands
-- =====================================================

INSERT INTO brands (name, slug, description, logo, created_at, updated_at) VALUES
('ASUS', 'asus', 'Thương hiệu laptop, mainboard, VGA hàng đầu thế giới', NULL, NOW(), NOW()),
('MSI', 'msi', 'Chuyên laptop gaming, mainboard, VGA cao cấp', NULL, NOW(), NOW()),
('Dell', 'dell', 'Laptop doanh nghiệp, workstation, gaming Alienware', NULL, NOW(), NOW()),
('HP', 'hp', 'Laptop văn phòng, gaming Omen, workstation', NULL, NOW(), NOW()),
('Lenovo', 'lenovo', 'ThinkPad, IdeaPad, Legion gaming', NULL, NOW(), NOW()),
('Acer', 'acer', 'Laptop gaming Predator, Aspire, Nitro', NULL, NOW(), NOW()),
('Apple', 'apple', 'MacBook Pro, MacBook Air, iMac', NULL, NOW(), NOW()),
('Gigabyte', 'gigabyte', 'Laptop gaming Aorus, mainboard, VGA', NULL, NOW(), NOW());

-- =====================================================
-- CPU Brands
-- =====================================================

INSERT INTO brands (name, slug, description, logo, created_at, updated_at) VALUES
('Intel', 'intel', 'CPU Core i3, i5, i7, i9, Xeon', NULL, NOW(), NOW()),
('AMD', 'amd', 'CPU Ryzen, Threadripper, EPYC', NULL, NOW(), NOW());

-- =====================================================
-- VGA Brands
-- =====================================================

INSERT INTO brands (name, slug, description, logo, created_at, updated_at) VALUES
('NVIDIA', 'nvidia', 'GeForce RTX, GTX, Quadro', NULL, NOW(), NOW()),
('ASUS ROG', 'asus-rog', 'VGA ROG Strix, TUF Gaming', NULL, NOW(), NOW()),
('MSI Gaming', 'msi-gaming', 'VGA Gaming X, Ventus, Suprim', NULL, NOW(), NOW()),
('Gigabyte Aorus', 'gigabyte-aorus', 'VGA Aorus Master, Gaming OC', NULL, NOW(), NOW()),
('EVGA', 'evga', 'VGA FTW3, XC Gaming', NULL, NOW(), NOW()),
('Zotac', 'zotac', 'VGA Gaming, AMP Extreme', NULL, NOW(), NOW()),
('Palit', 'palit', 'VGA GameRock, GamingPro', NULL, NOW(), NOW()),
('Colorful', 'colorful', 'VGA iGame, Battle Ax', NULL, NOW(), NOW()),
('Inno3D', 'inno3d', 'VGA iChill, Twin X2', NULL, NOW(), NOW());

-- =====================================================
-- RAM Brands
-- =====================================================

INSERT INTO brands (name, slug, description, logo, created_at, updated_at) VALUES
('Corsair', 'corsair', 'RAM Vengeance, Dominator Platinum', NULL, NOW(), NOW()),
('G.Skill', 'gskill', 'RAM Trident Z, Ripjaws', NULL, NOW(), NOW()),
('Kingston', 'kingston', 'RAM Fury, HyperX', NULL, NOW(), NOW()),
('Crucial', 'crucial', 'RAM Ballistix, Standard', NULL, NOW(), NOW()),
('TeamGroup', 'teamgroup', 'RAM T-Force, Elite', NULL, NOW(), NOW()),
('ADATA', 'adata', 'RAM XPG Spectrix', NULL, NOW(), NOW());

-- =====================================================
-- Storage Brands (SSD/HDD)
-- =====================================================

INSERT INTO brands (name, slug, description, logo, created_at, updated_at) VALUES
('Samsung', 'samsung', 'SSD 990 Pro, 980 Pro, 870 EVO', NULL, NOW(), NOW()),
('Western Digital', 'western-digital', 'SSD WD Black, Blue, HDD WD Blue, Red', NULL, NOW(), NOW()),
('Seagate', 'seagate', 'HDD Barracuda, IronWolf, FireCuda', NULL, NOW(), NOW()),
('Crucial MX', 'crucial-mx', 'SSD MX500, P3, P5', NULL, NOW(), NOW()),
('SK Hynix', 'sk-hynix', 'SSD Platinum P41, Gold P31', NULL, NOW(), NOW());

-- =====================================================
-- PSU Brands
-- =====================================================

INSERT INTO brands (name, slug, description, logo, created_at, updated_at) VALUES
('Corsair PSU', 'corsair-psu', 'Nguồn RM, HX, AX series', NULL, NOW(), NOW()),
('Cooler Master', 'cooler-master', 'Nguồn MWE, V Gold, V Platinum', NULL, NOW(), NOW()),
('Seasonic', 'seasonic', 'Nguồn Focus, Prime, Vertex', NULL, NOW(), NOW()),
('EVGA PSU', 'evga-psu', 'Nguồn SuperNOVA, BQ series', NULL, NOW(), NOW()),
('Thermaltake', 'thermaltake', 'Nguồn Toughpower, Smart', NULL, NOW(), NOW());

-- =====================================================
-- Case Brands
-- =====================================================

INSERT INTO brands (name, slug, description, logo, created_at, updated_at) VALUES
('NZXT', 'nzxt', 'Case H510, H710, S340', NULL, NOW(), NOW()),
('Lian Li', 'lian-li', 'Case O11 Dynamic, Lancool', NULL, NOW(), NOW()),
('Fractal Design', 'fractal-design', 'Case Meshify, Define, Torrent', NULL, NOW(), NOW()),
('Phanteks', 'phanteks', 'Case Eclipse, Enthoo', NULL, NOW(), NOW());

-- =====================================================
-- Cooling Brands
-- =====================================================

INSERT INTO brands (name, slug, description, logo, created_at, updated_at) VALUES
('Noctua', 'noctua', 'Tản nhiệt khí NH-D15, NH-U12S', NULL, NOW(), NOW()),
('be quiet!', 'be-quiet', 'Tản nhiệt Dark Rock, Pure Rock', NULL, NOW(), NOW()),
('Arctic', 'arctic', 'Tản nhiệt Freezer, Liquid Freezer', NULL, NOW(), NOW()),
('DeepCool', 'deepcool', 'Tản nhiệt AK620, Assassin', NULL, NOW(), NOW());

-- =====================================================
-- Peripheral Brands
-- =====================================================

INSERT INTO brands (name, slug, description, logo, created_at, updated_at) VALUES
('Logitech', 'logitech', 'Chuột, bàn phím, tai nghe gaming', NULL, NOW(), NOW()),
('Razer', 'razer', 'Gaming gear, RGB, Chroma', NULL, NOW(), NOW()),
('SteelSeries', 'steelseries', 'Gaming peripherals, Arctis', NULL, NOW(), NOW()),
('HyperX', 'hyperx', 'Tai nghe Cloud, bàn phím Alloy', NULL, NOW(), NOW()),
('Corsair Gaming', 'corsair-gaming', 'Bàn phím K70, chuột Dark Core', NULL, NOW(), NOW()),
('Keychron', 'keychron', 'Bàn phím cơ K2, K8, Q series', NULL, NOW(), NOW()),
('Akko', 'akko', 'Bàn phím cơ giá rẻ, keycap', NULL, NOW(), NOW()),
('Ducky', 'ducky', 'Bàn phím cơ One 2, One 3', NULL, NOW(), NOW());

-- =====================================================
-- Monitor Brands
-- =====================================================

INSERT INTO brands (name, slug, description, logo, created_at, updated_at) VALUES
('LG', 'lg', 'Màn hình UltraGear, UltraWide', NULL, NOW(), NOW()),
('BenQ', 'benq', 'Màn hình Zowie, Mobiuz gaming', NULL, NOW(), NOW()),
('ViewSonic', 'viewsonic', 'Màn hình Elite, VX series', NULL, NOW(), NOW()),
('AOC', 'aoc', 'Màn hình gaming, văn phòng', NULL, NOW(), NOW());

-- =====================================================
-- Network Brands
-- =====================================================

INSERT INTO brands (name, slug, description, logo, created_at, updated_at) VALUES
('TP-Link', 'tp-link', 'Router, Switch, Access Point', NULL, NOW(), NOW()),
('Asus Networking', 'asus-networking', 'Router ROG, RT-AX series', NULL, NOW(), NOW()),
('Netgear', 'netgear', 'Router Nighthawk, Orbi mesh', NULL, NOW(), NOW()),
('Ubiquiti', 'ubiquiti', 'UniFi, EdgeRouter enterprise', NULL, NOW(), NOW());

-- =====================================================
-- Summary
-- =====================================================
-- Total brands: 60+
-- Categories covered:
-- - Laptop & PC: 8 brands
-- - CPU: 2 brands
-- - VGA: 10 brands
-- - RAM: 6 brands
-- - Storage: 5 brands
-- - PSU: 5 brands
-- - Case: 4 brands
-- - Cooling: 4 brands
-- - Peripherals: 8 brands
-- - Monitors: 4 brands
-- - Network: 4 brands
-- =====================================================
