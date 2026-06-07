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
@Order(5)
@RequiredArgsConstructor
public class BrandDataSeeder implements CommandLineRunner {

    private final JdbcTemplate jdbc;

    @Override
    @Transactional
    public void run(String... args) {
        Long existing = jdbc.queryForObject("SELECT COUNT(*) FROM brands", Long.class);
        if (existing != null && existing > 0) {
            log.info("Brands already exist, skipping seed");
            return;
        }

        List<BrandSeed> brands = List.of(
                new BrandSeed("ASUS", "asus", "Thương hiệu laptop, mainboard, VGA hàng đầu thế giới"),
                new BrandSeed("MSI", "msi", "Chuyên laptop gaming, mainboard, VGA cao cấp"),
                new BrandSeed("Dell", "dell", "Laptop doanh nghiệp, workstation, gaming Alienware"),
                new BrandSeed("HP", "hp", "Laptop văn phòng, gaming Omen, workstation"),
                new BrandSeed("Lenovo", "lenovo", "ThinkPad, IdeaPad, Legion gaming"),
                new BrandSeed("Acer", "acer", "Laptop gaming Predator, Aspire, Nitro"),
                new BrandSeed("Apple", "apple", "MacBook Pro, MacBook Air, iMac"),
                new BrandSeed("Gigabyte", "gigabyte", "Laptop gaming Aorus, mainboard, VGA"),
                new BrandSeed("Intel", "intel", "CPU Core i3, i5, i7, i9, Xeon"),
                new BrandSeed("AMD", "amd", "CPU Ryzen, Threadripper, EPYC"),
                new BrandSeed("NVIDIA", "nvidia", "GeForce RTX, GTX, Quadro"),
                new BrandSeed("ASUS ROG", "asus-rog", "VGA ROG Strix, TUF Gaming"),
                new BrandSeed("MSI Gaming", "msi-gaming", "VGA Gaming X, Ventus, Suprim"),
                new BrandSeed("Gigabyte Aorus", "gigabyte-aorus", "VGA Aorus Master, Gaming OC"),
                new BrandSeed("EVGA", "evga", "VGA FTW3, XC Gaming"),
                new BrandSeed("Zotac", "zotac", "VGA Gaming, AMP Extreme"),
                new BrandSeed("Palit", "palit", "VGA GameRock, GamingPro"),
                new BrandSeed("Colorful", "colorful", "VGA iGame, Battle Ax"),
                new BrandSeed("Inno3D", "inno3d", "VGA iChill, Twin X2"),
                new BrandSeed("Corsair", "corsair", "RAM Vengeance, Dominator Platinum"),
                new BrandSeed("G.Skill", "gskill", "RAM Trident Z, Ripjaws"),
                new BrandSeed("Kingston", "kingston", "RAM Fury, HyperX"),
                new BrandSeed("Crucial", "crucial", "RAM Ballistix, Standard"),
                new BrandSeed("TeamGroup", "teamgroup", "RAM T-Force, Elite"),
                new BrandSeed("ADATA", "adata", "RAM XPG Spectrix"),
                new BrandSeed("Samsung", "samsung", "SSD 990 Pro, 980 Pro, 870 EVO"),
                new BrandSeed("Western Digital", "western-digital", "SSD WD Black, Blue, HDD WD Blue, Red"),
                new BrandSeed("Seagate", "seagate", "HDD Barracuda, IronWolf, FireCuda"),
                new BrandSeed("Crucial MX", "crucial-mx", "SSD MX500, P3, P5"),
                new BrandSeed("SK Hynix", "sk-hynix", "SSD Platinum P41, Gold P31"),
                new BrandSeed("Corsair PSU", "corsair-psu", "Nguồn RM, HX, AX series"),
                new BrandSeed("Cooler Master", "cooler-master", "Nguồn MWE, V Gold, V Platinum"),
                new BrandSeed("Seasonic", "seasonic", "Nguồn Focus, Prime, Vertex"),
                new BrandSeed("EVGA PSU", "evga-psu", "Nguồn SuperNOVA, BQ series"),
                new BrandSeed("Thermaltake", "thermaltake", "Nguồn Toughpower, Smart"),
                new BrandSeed("NZXT", "nzxt", "Case H510, H710, S340"),
                new BrandSeed("Lian Li", "lian-li", "Case O11 Dynamic, Lancool"),
                new BrandSeed("Fractal Design", "fractal-design", "Case Meshify, Define, Torrent"),
                new BrandSeed("Phanteks", "phanteks", "Case Eclipse, Enthoo"),
                new BrandSeed("Noctua", "noctua", "Tản nhiệt khí NH-D15, NH-U12S"),
                new BrandSeed("be quiet!", "be-quiet", "Tản nhiệt Dark Rock, Pure Rock"),
                new BrandSeed("Arctic", "arctic", "Tản nhiệt Freezer, Liquid Freezer"),
                new BrandSeed("DeepCool", "deepcool", "Tản nhiệt AK620, Assassin"),
                new BrandSeed("Logitech", "logitech", "Chuột, bàn phím, tai nghe gaming"),
                new BrandSeed("Razer", "razer", "Gaming gear, RGB, Chroma"),
                new BrandSeed("SteelSeries", "steelseries", "Gaming peripherals, Arctis"),
                new BrandSeed("HyperX", "hyperx", "Tai nghe Cloud, bàn phím Alloy"),
                new BrandSeed("Corsair Gaming", "corsair-gaming", "Bàn phím K70, chuột Dark Core"),
                new BrandSeed("Keychron", "keychron", "Bàn phím cơ K2, K8, Q series"),
                new BrandSeed("Akko", "akko", "Bàn phím cơ giá rẻ, keycap"),
                new BrandSeed("Ducky", "ducky", "Bàn phím cơ One 2, One 3"),
                new BrandSeed("LG", "lg", "Màn hình UltraGear, UltraWide"),
                new BrandSeed("BenQ", "benq", "Màn hình Zowie, Mobiuz gaming"),
                new BrandSeed("ViewSonic", "viewsonic", "Màn hình Elite, VX series"),
                new BrandSeed("AOC", "aoc", "Màn hình gaming, văn phòng"),
                new BrandSeed("TP-Link", "tp-link", "Router, Switch, Access Point"),
                new BrandSeed("Asus Networking", "asus-networking", "Router ROG, RT-AX series"),
                new BrandSeed("Netgear", "netgear", "Router Nighthawk, Orbi mesh"),
                new BrandSeed("Ubiquiti", "ubiquiti", "UniFi, EdgeRouter enterprise")
        );

        jdbc.batchUpdate("""
                INSERT INTO brands (name, slug, description, logo)
                VALUES (?, ?, ?, NULL)
                """, brands, brands.size(), (ps, brand) -> {
            ps.setString(1, brand.name());
            ps.setString(2, brand.slug());
            ps.setString(3, brand.description());
        });

        log.info("Seeded {} brands", brands.size());
    }

    private record BrandSeed(String name, String slug, String description) {}
}
