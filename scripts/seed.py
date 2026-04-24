"""
Script seed data — tạo hàng loạt categories, brands, products.

Cách dùng:
  python scripts/seed.py                    # seed tất cả (10 brands, 10 cats, 30 products)
  python scripts/seed.py --products 50      # tạo 50 sản phẩm
  python scripts/seed.py --brands 5 --categories 5 --products 20
"""

import sys
import os
import time
import argparse
import random

sys.path.insert(0, os.path.dirname(__file__))
from capture.base import login, call, BASE_URL

# ── Config ────────────────────────────────────────────────────────────────────
ADMIN_EMAIL    = "flourinee@gmail.com"
ADMIN_PASSWORD = "flourinee@gmail.com"

# ── Sample data ───────────────────────────────────────────────────────────────
BRAND_NAMES = [
    "Nike", "Adidas", "Puma", "Reebok", "New Balance",
    "Under Armour", "Converse", "Vans", "Fila", "Asics",
    "Skechers", "Timberland", "Columbia", "The North Face", "Patagonia",
]

CATEGORY_NAMES = [
    "Áo thun", "Quần jeans", "Giày thể thao", "Túi xách", "Phụ kiện",
    "Áo khoác", "Quần short", "Giày sandal", "Mũ nón", "Tất vớ",
    "Áo sơ mi", "Váy đầm", "Giày cao gót", "Balo", "Đồng hồ",
]

PRODUCT_TEMPLATES = [
    {"name": "Áo thun basic {brand}", "price_base": 150000},
    {"name": "Quần jeans slim {brand}", "price_base": 450000},
    {"name": "Giày chạy bộ {brand} Pro", "price_base": 800000},
    {"name": "Túi tote {brand} canvas", "price_base": 250000},
    {"name": "Áo khoác gió {brand}", "price_base": 600000},
    {"name": "Quần short thể thao {brand}", "price_base": 200000},
    {"name": "Giày sneaker {brand} Classic", "price_base": 950000},
    {"name": "Balo {brand} 20L", "price_base": 700000},
    {"name": "Mũ lưỡi trai {brand}", "price_base": 120000},
    {"name": "Áo polo {brand}", "price_base": 280000},
    {"name": "Giày sandal {brand} Sport", "price_base": 350000},
    {"name": "Áo hoodie {brand}", "price_base": 500000},
    {"name": "Quần legging {brand}", "price_base": 220000},
    {"name": "Giày đi bộ {brand} Comfort", "price_base": 650000},
    {"name": "Túi đeo chéo {brand}", "price_base": 320000},
]

SIZES   = ["XS", "S", "M", "L", "XL", "XXL"]
COLORS  = ["Đen", "Trắng", "Xám", "Xanh navy", "Đỏ", "Xanh lá", "Vàng", "Hồng"]


def slugify(text: str) -> str:
    import unicodedata, re
    text = unicodedata.normalize("NFD", text)
    text = "".join(c for c in text if unicodedata.category(c) != "Mn")
    text = text.lower().strip()
    text = re.sub(r"[^a-z0-9\s-]", "", text)
    text = re.sub(r"[\s]+", "-", text)
    return text


def seed_brands(s, count: int) -> list[dict]:
    print(f"\n[brands] Tạo {count} brands...")
    created = []
    ts = int(time.time())
    names = random.sample(BRAND_NAMES * 3, min(count, len(BRAND_NAMES) * 3))[:count]

    for i, name in enumerate(names):
        slug = f"{slugify(name)}-{ts}-{i}"
        r = call(s, "post", "/api/brands", {
            "name": f"{name} ({ts})" if i >= len(BRAND_NAMES) else name,
            "slug": slug,
            "active": True,
        })
        if r["status"] in (200, 201):
            brand = r["body"].get("data", {})
            created.append(brand)
            print(f"  ✓ Brand: {brand.get('name')} (id={brand.get('id')})")
        else:
            print(f"  ✗ Brand '{name}': {r['status']} — {r['body']}")

    return created


def seed_categories(s, count: int) -> list[dict]:
    print(f"\n[categories] Tạo {count} categories...")
    created = []
    ts = int(time.time())
    names = random.sample(CATEGORY_NAMES * 3, min(count, len(CATEGORY_NAMES) * 3))[:count]

    for i, name in enumerate(names):
        slug = f"{slugify(name)}-{ts}-{i}"
        r = call(s, "post", "/api/categories", {
            "name": f"{name} ({ts})" if i >= len(CATEGORY_NAMES) else name,
            "slug": slug,
            "active": True,
        })
        if r["status"] in (200, 201):
            cat = r["body"].get("data", {})
            created.append(cat)
            print(f"  ✓ Category: {cat.get('name')} (id={cat.get('id')})")
        else:
            print(f"  ✗ Category '{name}': {r['status']} — {r['body']}")

    return created


def seed_products(s, count: int, brands: list, categories: list):
    print(f"\n[products] Tạo {count} sản phẩm...")
    ts = int(time.time())
    success = 0

    for i in range(count):
        brand   = random.choice(brands)   if brands   else None
        cat     = random.choice(categories) if categories else None
        tmpl    = PRODUCT_TEMPLATES[i % len(PRODUCT_TEMPLATES)]
        brand_name = brand.get("name", "Generic") if brand else "Generic"

        name  = tmpl["name"].format(brand=brand_name)
        slug  = f"{slugify(name)}-{ts}-{i}"
        price = tmpl["price_base"] + random.randint(-50000, 200000)

        # Tạo 2-4 variants ngẫu nhiên
        num_variants = random.randint(2, 4)
        chosen_sizes  = random.sample(SIZES, min(num_variants, len(SIZES)))
        chosen_color  = random.choice(COLORS)
        variants = [
            {
                "sku":        f"SKU-{ts}-{i}-{j}",
                "name":       f"{size} / {chosen_color}",
                "price":      price + j * 20000,
                "attributes": {"size": size, "color": chosen_color},
                "active":     True,
            }
            for j, size in enumerate(chosen_sizes)
        ]

        payload = {
            "name":        name,
            "slug":        slug,
            "description": f"Sản phẩm {name} chất lượng cao.",
            "thumbnail":   f"https://picsum.photos/seed/{ts}{i}/400/400",
            "active":      True,
            "variants":    variants,
        }
        if cat:
            payload["categoryId"] = cat.get("id")
        if brand:
            payload["brandId"] = brand.get("id")

        r = call(s, "post", "/api/products", payload)
        if r["status"] in (200, 201):
            p = r["body"].get("data", {})
            success += 1
            print(f"  ✓ [{i+1}/{count}] {p.get('name')} (id={p.get('id')}, {len(variants)} variants)")
        else:
            print(f"  ✗ [{i+1}/{count}] '{name}': {r['status']} — {r['body']}")

    print(f"\n  Tạo thành công {success}/{count} sản phẩm.")


# ── Main ──────────────────────────────────────────────────────────────────────
if __name__ == "__main__":
    parser = argparse.ArgumentParser(description="Seed data cho API")
    parser.add_argument("--brands",     type=int, default=10, help="Số brands cần tạo (default: 10)")
    parser.add_argument("--categories", type=int, default=10, help="Số categories cần tạo (default: 10)")
    parser.add_argument("--products",   type=int, default=30, help="Số products cần tạo (default: 30)")
    parser.add_argument("--skip-brands",     action="store_true", help="Bỏ qua tạo brands mới, dùng brands có sẵn")
    parser.add_argument("--skip-categories", action="store_true", help="Bỏ qua tạo categories mới, dùng categories có sẵn")
    args = parser.parse_args()

    print(f"Base URL : {BASE_URL}")
    print(f"Account  : {ADMIN_EMAIL}")

    s = login(ADMIN_EMAIL, ADMIN_PASSWORD)
    if not s.cookies.get("SESSION"):
        print("ERROR: Login failed. Kiểm tra ADMIN_EMAIL / ADMIN_PASSWORD trong script.")
        sys.exit(1)
    print(f"Logged in. SESSION={s.cookies.get('SESSION')[:20]}...")

    # Lấy brands/categories có sẵn hoặc tạo mới
    if args.skip_brands:
        r = call(s, "get", "/api/brands", params={"active": "true", "size": "100"})
        brands = r["body"].get("data", {}).get("content", [])
        print(f"\n[brands] Dùng {len(brands)} brands có sẵn.")
    else:
        brands = seed_brands(s, args.brands)

    if args.skip_categories:
        r = call(s, "get", "/api/categories", params={"active": "true", "size": "100"})
        categories = r["body"].get("data", {}).get("content", [])
        print(f"\n[categories] Dùng {len(categories)} categories có sẵn.")
    else:
        categories = seed_categories(s, args.categories)

    seed_products(s, args.products, brands, categories)

    print("\nDone!")
