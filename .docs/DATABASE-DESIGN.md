# Database Design — Nitrotech API

## Nguyên tắc chung

- Tất cả bảng có `created_at`, `updated_at`
- Bảng quan trọng có `deleted_at` (soft delete)
- Không dùng prefix `is_` cho boolean column
- Dùng `BIGSERIAL` cho primary key (PostgreSQL)
- Boolean trạng thái dùng thống nhất tên `active`
- Enum dùng `VARCHAR` + CHECK CONSTRAINT
- Product variant dùng `JSONB` cho attributes (GIN index)

---

## Soft Delete

| Bảng | Soft delete |
|------|-------------|
| `users` | ✅ |
| `products` | ✅ |
| `product_variants` | ✅ |
| `categories` | ✅ |
| `brands` | ✅ |
| `orders` | ✅ |
| `roles` | ✅ |
| `permissions` | ✅ |
| `reviews` | ✅ |
| Các bảng còn lại | ❌ |

---

## Schema

### users
```sql
CREATE TABLE users (
    id          BIGSERIAL    PRIMARY KEY,
    name        VARCHAR(255) NOT NULL,
    email       VARCHAR(255) NOT NULL UNIQUE,
    password    VARCHAR(255),
    phone       VARCHAR(20),
    avatar      VARCHAR(500),
    status      VARCHAR(20)  NOT NULL DEFAULT 'inactive',  -- inactive | active | banned | suspended
    provider    VARCHAR(20)  NOT NULL DEFAULT 'local',     -- local | google | facebook
    provider_id VARCHAR(100),
    created_at  TIMESTAMP    NOT NULL DEFAULT NOW(),
    updated_at  TIMESTAMP    NOT NULL DEFAULT NOW(),
    deleted_at  TIMESTAMP
);
```

---

### roles & permissions
```sql
CREATE TABLE roles (
    id          BIGSERIAL    PRIMARY KEY,
    name        VARCHAR(100) NOT NULL,
    slug        VARCHAR(100) NOT NULL UNIQUE,
    description VARCHAR(500),
    active      BOOLEAN      NOT NULL DEFAULT TRUE,
    created_at  TIMESTAMP    NOT NULL DEFAULT NOW(),
    updated_at  TIMESTAMP    NOT NULL DEFAULT NOW(),
    deleted_at  TIMESTAMP
);

CREATE TABLE permissions (
    id          BIGSERIAL    PRIMARY KEY,
    name        VARCHAR(100) NOT NULL,
    slug        VARCHAR(100) NOT NULL UNIQUE,   -- product:create | order:delete
    description VARCHAR(500),
    group_name  VARCHAR(100),                   -- product | order | user | ...
    created_at  TIMESTAMP    NOT NULL DEFAULT NOW(),
    updated_at  TIMESTAMP    NOT NULL DEFAULT NOW(),
    deleted_at  TIMESTAMP
);

CREATE TABLE role_permissions (
    role_id       BIGINT NOT NULL,
    permission_id BIGINT NOT NULL,
    PRIMARY KEY (role_id, permission_id),
    CONSTRAINT fk_rp_role       FOREIGN KEY (role_id)       REFERENCES roles(id)       ON DELETE CASCADE,
    CONSTRAINT fk_rp_permission FOREIGN KEY (permission_id) REFERENCES permissions(id) ON DELETE CASCADE
);

CREATE TABLE user_roles (
    user_id BIGINT NOT NULL,
    role_id BIGINT NOT NULL,
    PRIMARY KEY (user_id, role_id),
    CONSTRAINT fk_ur_user FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE,
    CONSTRAINT fk_ur_role FOREIGN KEY (role_id) REFERENCES roles(id) ON DELETE CASCADE
);
```

---

### addresses
```sql
CREATE TABLE addresses (
    id              BIGSERIAL    PRIMARY KEY,
    user_id         BIGINT       NOT NULL,
    receiver        VARCHAR(255) NOT NULL,
    phone           VARCHAR(20)  NOT NULL,
    province        VARCHAR(100) NOT NULL,
    province_code   VARCHAR(20)  NOT NULL,
    district        VARCHAR(100) NOT NULL,
    district_code   VARCHAR(20)  NOT NULL,
    ward            VARCHAR(100) NOT NULL,
    ward_code       VARCHAR(20)  NOT NULL,
    street          VARCHAR(255) NOT NULL,
    default_address BOOLEAN      NOT NULL DEFAULT FALSE,
    created_at      TIMESTAMP    NOT NULL DEFAULT NOW(),
    updated_at      TIMESTAMP    NOT NULL DEFAULT NOW(),
    CONSTRAINT fk_address_user FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE
);
```

---

### brands
```sql
CREATE TABLE brands (
    id          BIGSERIAL    PRIMARY KEY,
    name        VARCHAR(255) NOT NULL,
    slug        VARCHAR(255) NOT NULL UNIQUE,
    logo        VARCHAR(500),
    description TEXT,
    active      BOOLEAN      NOT NULL DEFAULT TRUE,
    created_at  TIMESTAMP    NOT NULL DEFAULT NOW(),
    updated_at  TIMESTAMP    NOT NULL DEFAULT NOW(),
    deleted_at  TIMESTAMP
);
```

---

### categories
```sql
CREATE TABLE categories (
    id          BIGSERIAL    PRIMARY KEY,
    name        VARCHAR(255) NOT NULL,
    slug        VARCHAR(255) NOT NULL UNIQUE,
    description TEXT,
    image       VARCHAR(500),
    parent_id   BIGINT,
    active      BOOLEAN      NOT NULL DEFAULT TRUE,
    created_at  TIMESTAMP    NOT NULL DEFAULT NOW(),
    updated_at  TIMESTAMP    NOT NULL DEFAULT NOW(),
    deleted_at  TIMESTAMP,
    CONSTRAINT fk_category_parent FOREIGN KEY (parent_id) REFERENCES categories(id) ON DELETE SET NULL
);
```

---

### products & variants
```sql
CREATE TABLE products (
    id          BIGSERIAL    PRIMARY KEY,
    category_id BIGINT       NOT NULL,
    brand_id    BIGINT,
    name        VARCHAR(255) NOT NULL,
    slug        VARCHAR(255) NOT NULL UNIQUE,
    description TEXT,
    thumbnail   VARCHAR(500),
    specs       JSONB,                          -- thông số kỹ thuật chung
    active      BOOLEAN      NOT NULL DEFAULT TRUE,
    created_at  TIMESTAMP    NOT NULL DEFAULT NOW(),
    updated_at  TIMESTAMP    NOT NULL DEFAULT NOW(),
    deleted_at  TIMESTAMP,
    CONSTRAINT fk_product_category FOREIGN KEY (category_id) REFERENCES categories(id),
    CONSTRAINT fk_product_brand    FOREIGN KEY (brand_id)    REFERENCES brands(id)
);

CREATE TABLE product_images (
    id         BIGSERIAL    PRIMARY KEY,
    product_id BIGINT       NOT NULL,
    url        VARCHAR(500) NOT NULL,
    sort_order INT          NOT NULL DEFAULT 0,
    created_at TIMESTAMP    NOT NULL DEFAULT NOW(),
    CONSTRAINT fk_image_product FOREIGN KEY (product_id) REFERENCES products(id) ON DELETE CASCADE
);

CREATE TABLE product_variants (
    id         BIGSERIAL      PRIMARY KEY,
    product_id BIGINT         NOT NULL,
    sku        VARCHAR(100)   NOT NULL UNIQUE,
    name       VARCHAR(255)   NOT NULL,         -- "i7 / 32GB / 512GB / Silver"
    price      DECIMAL(15, 2) NOT NULL,
    attributes JSONB,                           -- {"cpu": "i7-13700H", "ram_gb": 32, "storage_gb": 512}
    created_at TIMESTAMP      NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMP      NOT NULL DEFAULT NOW(),
    deleted_at TIMESTAMP,
    CONSTRAINT fk_variant_product FOREIGN KEY (product_id) REFERENCES products(id) ON DELETE CASCADE
);

CREATE INDEX idx_variant_attributes ON product_variants USING GIN (attributes);
```

---

### reviews
```sql
CREATE TABLE reviews (
    id         BIGSERIAL PRIMARY KEY,
    product_id BIGINT    NOT NULL,
    user_id    BIGINT    NOT NULL,
    order_id   BIGINT    NOT NULL,              -- chỉ user đã mua mới review được
    rating     SMALLINT  NOT NULL CHECK (rating BETWEEN 1 AND 5),
    comment    TEXT,
    status     VARCHAR(20) NOT NULL DEFAULT 'pending',  -- pending | approved | rejected
    created_at TIMESTAMP   NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMP   NOT NULL DEFAULT NOW(),
    deleted_at TIMESTAMP,
    UNIQUE (product_id, user_id, order_id),     -- mỗi order chỉ review 1 lần
    CONSTRAINT fk_review_product FOREIGN KEY (product_id) REFERENCES products(id),
    CONSTRAINT fk_review_user    FOREIGN KEY (user_id)    REFERENCES users(id),
    CONSTRAINT fk_review_order   FOREIGN KEY (order_id)   REFERENCES orders(id)
);
```

---

### carts
```sql
CREATE TABLE carts (
    id         BIGSERIAL PRIMARY KEY,
    user_id    BIGINT    NOT NULL UNIQUE,       -- 1 user 1 cart
    created_at TIMESTAMP NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMP NOT NULL DEFAULT NOW(),
    CONSTRAINT fk_cart_user FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE
);

CREATE TABLE cart_items (
    id         BIGSERIAL      PRIMARY KEY,
    cart_id    BIGINT         NOT NULL,
    variant_id BIGINT         NOT NULL,
    quantity   INT            NOT NULL DEFAULT 1,
    created_at TIMESTAMP      NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMP      NOT NULL DEFAULT NOW(),
    UNIQUE (cart_id, variant_id),
    CONSTRAINT fk_ci_cart    FOREIGN KEY (cart_id)    REFERENCES carts(id)            ON DELETE CASCADE,
    CONSTRAINT fk_ci_variant FOREIGN KEY (variant_id) REFERENCES product_variants(id) ON DELETE CASCADE
);
```

---

### orders & order_items
```sql
CREATE TABLE orders (
    id               BIGSERIAL      PRIMARY KEY,
    user_id          BIGINT         NOT NULL,
    shipping_address JSONB          NOT NULL,   -- snapshot địa chỉ lúc đặt hàng
    status           VARCHAR(20)    NOT NULL DEFAULT 'pending',
    -- pending | confirmed | processing | shipped | delivered | cancelled | refunded
    total_amount     DECIMAL(15, 2) NOT NULL,
    discount_amount  DECIMAL(15, 2) NOT NULL DEFAULT 0,
    shipping_fee     DECIMAL(15, 2) NOT NULL DEFAULT 0,
    final_amount     DECIMAL(15, 2) NOT NULL,
    coupon_code      VARCHAR(50),
    note             TEXT,
    created_at       TIMESTAMP      NOT NULL DEFAULT NOW(),
    updated_at       TIMESTAMP      NOT NULL DEFAULT NOW(),
    deleted_at       TIMESTAMP,
    CONSTRAINT fk_order_user FOREIGN KEY (user_id) REFERENCES users(id)
);

CREATE TABLE order_items (
    id         BIGSERIAL      PRIMARY KEY,
    order_id   BIGINT         NOT NULL,
    variant_id BIGINT         NOT NULL,
    name       VARCHAR(255)   NOT NULL,         -- snapshot tên variant
    sku        VARCHAR(100)   NOT NULL,         -- snapshot SKU
    quantity   INT            NOT NULL,
    unit_price DECIMAL(15, 2) NOT NULL,
    subtotal   DECIMAL(15, 2) NOT NULL,
    created_at TIMESTAMP      NOT NULL DEFAULT NOW(),
    CONSTRAINT fk_item_order   FOREIGN KEY (order_id)   REFERENCES orders(id)           ON DELETE CASCADE,
    CONSTRAINT fk_item_variant FOREIGN KEY (variant_id) REFERENCES product_variants(id)
);
```

---

### payments
```sql
CREATE TABLE payment_transactions (
    id              BIGSERIAL      PRIMARY KEY,
    order_id        BIGINT         NOT NULL,
    provider        VARCHAR(20)    NOT NULL,    -- cod | vnpay | momo
    amount          DECIMAL(15, 2) NOT NULL,
    status          VARCHAR(20)    NOT NULL DEFAULT 'pending',
    -- pending | success | failed | refunded
    provider_ref    VARCHAR(255),               -- mã giao dịch từ VNPay/MoMo
    provider_data   JSONB,                      -- raw response từ provider
    paid_at         TIMESTAMP,
    created_at      TIMESTAMP      NOT NULL DEFAULT NOW(),
    updated_at      TIMESTAMP      NOT NULL DEFAULT NOW(),
    CONSTRAINT fk_payment_order FOREIGN KEY (order_id) REFERENCES orders(id)
);
```

---

### shipments
```sql
CREATE TABLE shipments (
    id           BIGSERIAL      PRIMARY KEY,
    order_id     BIGINT         NOT NULL UNIQUE,
    provider     VARCHAR(20)    NOT NULL,       -- ghn | ghtk | self
    tracking_code VARCHAR(100),
    status       VARCHAR(30)    NOT NULL DEFAULT 'pending',
    -- pending | picked_up | in_transit | delivered | failed | returned
    fee          DECIMAL(15, 2) NOT NULL DEFAULT 0,
    estimated_at TIMESTAMP,
    shipped_at   TIMESTAMP,
    delivered_at TIMESTAMP,
    created_at   TIMESTAMP      NOT NULL DEFAULT NOW(),
    updated_at   TIMESTAMP      NOT NULL DEFAULT NOW(),
    CONSTRAINT fk_shipment_order FOREIGN KEY (order_id) REFERENCES orders(id)
);

CREATE TABLE shipment_logs (
    id          BIGSERIAL    PRIMARY KEY,
    shipment_id BIGINT       NOT NULL,
    status      VARCHAR(30)  NOT NULL,
    note        TEXT,
    created_at  TIMESTAMP    NOT NULL DEFAULT NOW(),
    CONSTRAINT fk_log_shipment FOREIGN KEY (shipment_id) REFERENCES shipments(id) ON DELETE CASCADE
);
```

---

### promotions
```sql
CREATE TABLE promotions (
    id                  BIGSERIAL      PRIMARY KEY,
    name                VARCHAR(255)   NOT NULL,
    description         TEXT,
    code                VARCHAR(50)    UNIQUE,          -- NULL = sale tự động, có giá trị = voucher code
    type                VARCHAR(20)    NOT NULL,        -- percentage | fixed | freeship
    discount_value      DECIMAL(15, 2) NOT NULL,
    min_order_amount    DECIMAL(15, 2) NOT NULL DEFAULT 0,
    max_discount_amount DECIMAL(15, 2),                -- giới hạn giảm tối đa, quan trọng với percentage
    stackable           BOOLEAN        NOT NULL DEFAULT FALSE,
    priority            INT            NOT NULL DEFAULT 0,
    usage_limit         INT,                           -- NULL = unlimited
    usage_per_user      INT            NOT NULL DEFAULT 1,
    start_at            TIMESTAMP      NOT NULL,
    end_at              TIMESTAMP      NOT NULL,
    status              VARCHAR(20)    NOT NULL DEFAULT 'draft',  -- draft | active | paused | ended
    created_at          TIMESTAMP      NOT NULL DEFAULT NOW(),
    updated_at          TIMESTAMP      NOT NULL DEFAULT NOW()
);

CREATE TABLE promotion_targets (
    id           BIGSERIAL   PRIMARY KEY,
    promotion_id BIGINT      NOT NULL,
    target_type  VARCHAR(20) NOT NULL,                 -- product | variant | category | brand | order
    target_id    BIGINT      NOT NULL,
    excluded     BOOLEAN     NOT NULL DEFAULT FALSE,   -- FALSE = include, TRUE = exclude
    CONSTRAINT fk_pt_promotion FOREIGN KEY (promotion_id) REFERENCES promotions(id) ON DELETE CASCADE
);

CREATE INDEX idx_pt_lookup ON promotion_targets (promotion_id, target_type, target_id);

CREATE TABLE promotion_usages (
    id              BIGSERIAL      PRIMARY KEY,
    promotion_id    BIGINT         NOT NULL,
    user_id         BIGINT         NOT NULL,
    order_id        BIGINT         NOT NULL,
    used_code       VARCHAR(50),                       -- lưu lại code đã dùng nếu có
    discount_amount DECIMAL(15, 2) NOT NULL,
    created_at      TIMESTAMP      NOT NULL DEFAULT NOW(),
    CONSTRAINT fk_pu_promotion FOREIGN KEY (promotion_id) REFERENCES promotions(id),
    CONSTRAINT fk_pu_user      FOREIGN KEY (user_id)      REFERENCES users(id),
    CONSTRAINT fk_pu_order     FOREIGN KEY (order_id)     REFERENCES orders(id)
);

-- Index để check usage_limit và usage_per_user nhanh
CREATE INDEX idx_pu_lookup ON promotion_usages (promotion_id, user_id);
```

---

### banners
```sql
CREATE TABLE banners (
    id         BIGSERIAL    PRIMARY KEY,
    title      VARCHAR(255) NOT NULL,
    image      VARCHAR(500) NOT NULL,
    url        VARCHAR(500),
    position   VARCHAR(50)  NOT NULL,           -- home_hero | home_secondary | sidebar
    active     BOOLEAN      NOT NULL DEFAULT TRUE,
    start_date TIMESTAMP,
    end_date   TIMESTAMP,
    sort_order INT          NOT NULL DEFAULT 0,
    created_at TIMESTAMP    NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMP    NOT NULL DEFAULT NOW()
);
```

---

### inventories
```sql
CREATE TABLE inventories (
    id                  BIGSERIAL PRIMARY KEY,
    variant_id          BIGINT    NOT NULL UNIQUE,
    quantity            INT       NOT NULL DEFAULT 0,
    low_stock_threshold INT       NOT NULL DEFAULT 5,
    created_at          TIMESTAMP NOT NULL DEFAULT NOW(),
    updated_at          TIMESTAMP NOT NULL DEFAULT NOW(),
    CONSTRAINT fk_inventory_variant FOREIGN KEY (variant_id) REFERENCES product_variants(id) ON DELETE CASCADE
);
```

---

### wishlists
```sql
CREATE TABLE wishlists (
    user_id    BIGINT    NOT NULL,
    product_id BIGINT    NOT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT NOW(),
    PRIMARY KEY (user_id, product_id),
    CONSTRAINT fk_wl_user    FOREIGN KEY (user_id)    REFERENCES users(id)    ON DELETE CASCADE,
    CONSTRAINT fk_wl_product FOREIGN KEY (product_id) REFERENCES products(id) ON DELETE CASCADE
);
```

---

## Migration order

```
V1__create_users.sql
V2__create_roles_permissions.sql
V3__create_addresses.sql
V4__create_brands.sql
V5__create_categories.sql
V6__create_products.sql
V7__create_carts.sql
V8__create_orders.sql
V9__create_payments_shipments.sql
V10__create_inventories.sql
V11__create_promotions.sql
V12__create_reviews.sql
V13__create_banners.sql
V14__create_wishlists.sql
V15__seed_roles_permissions.sql
```
