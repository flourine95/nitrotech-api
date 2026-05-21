# Database Design — Nitrotech API

## Design Principles

- All tables have `created_at`, `updated_at` timestamps
- Important tables have `deleted_at` for soft delete
- No `is_` prefix for boolean columns
- Use `BIGSERIAL` for primary keys (PostgreSQL)
- Boolean status fields consistently named `active`
- Enums use `VARCHAR` or JPA `@Enumerated(EnumType.STRING)`
- Product variants use `JSONB` for attributes with GIN index

---

## Soft Delete

| Table | Soft delete |
|-------|-------------|
| `users` | Yes |
| `products` | Yes |
| `product_variants` | Yes |
| `categories` | Yes |
| `brands` | Yes |
| `orders` | Yes |
| `reviews` | Yes |
| Other tables | No |

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
    status      VARCHAR(20)  NOT NULL DEFAULT 'inactive',  -- inactive, active, banned, suspended
    provider    VARCHAR(20)  NOT NULL DEFAULT 'local',     -- local, google, facebook
    provider_id VARCHAR(100),
    created_at  TIMESTAMP    NOT NULL DEFAULT NOW(),
    updated_at  TIMESTAMP    NOT NULL DEFAULT NOW(),
    deleted_at  TIMESTAMP
);
```

---

### user_tokens
```sql
CREATE TABLE user_tokens (
    id         BIGSERIAL    PRIMARY KEY,
    user_id    BIGINT       NOT NULL,
    token      VARCHAR(255) NOT NULL UNIQUE,
    type       VARCHAR(30)  NOT NULL,           -- password_reset, email_verification
    expires_at TIMESTAMP    NOT NULL,
    used       BOOLEAN      NOT NULL DEFAULT FALSE,
    created_at TIMESTAMP    NOT NULL DEFAULT NOW(),
    CONSTRAINT fk_token_user FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE
);

CREATE INDEX idx_token_lookup ON user_tokens (token, type) WHERE NOT used;
```

---

### roles & permissions (not implemented)
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
    slug        VARCHAR(100) NOT NULL UNIQUE,   -- product:create, order:delete
    description VARCHAR(500),
    group_name  VARCHAR(100),                   -- product, order, user, etc.
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
    sort_order  INT          NOT NULL DEFAULT 0,
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
    id                        BIGSERIAL    PRIMARY KEY,
    category_id               BIGINT       NOT NULL,
    brand_id                  BIGINT,
    name                      VARCHAR(255) NOT NULL,
    slug                      VARCHAR(255) NOT NULL UNIQUE,
    description               TEXT,
    thumbnail                 VARCHAR(500),
    specs                     JSONB,                          -- general specifications
    active                    BOOLEAN      NOT NULL DEFAULT TRUE,
    manual_badge              VARCHAR(50),                    -- manual badge: "HOT", "NEW", "SALE"
    manual_badge_expires_at   TIMESTAMP,                      -- badge expiration time
    created_at                TIMESTAMP    NOT NULL DEFAULT NOW(),
    updated_at                TIMESTAMP    NOT NULL DEFAULT NOW(),
    deleted_at                TIMESTAMP,
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
    image_id   BIGINT,                          -- variant thumbnail image
    active     BOOLEAN        NOT NULL DEFAULT TRUE,
    created_at TIMESTAMP      NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMP      NOT NULL DEFAULT NOW(),
    deleted_at TIMESTAMP,
    CONSTRAINT fk_variant_product FOREIGN KEY (product_id) REFERENCES products(id) ON DELETE CASCADE,
    CONSTRAINT fk_variant_image   FOREIGN KEY (image_id)   REFERENCES product_images(id) ON DELETE SET NULL
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
    order_id   BIGINT    NOT NULL,              -- only users who purchased can review
    rating     SMALLINT  NOT NULL CHECK (rating BETWEEN 1 AND 5),
    comment    TEXT,
    images     JSONB,                            -- review image URLs: ["url1", "url2"]
    status     VARCHAR(20) NOT NULL DEFAULT 'pending',  -- pending, approved, rejected
    created_at TIMESTAMP   NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMP   NOT NULL DEFAULT NOW(),
    deleted_at TIMESTAMP,
    UNIQUE (product_id, user_id, order_id),     -- one review per order
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
    user_id    BIGINT    NOT NULL UNIQUE,       -- one cart per user
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
    shipping_address JSONB          NOT NULL,   -- address snapshot at order time
    status           VARCHAR(20)    NOT NULL DEFAULT 'pending',
    -- pending, confirmed, processing, shipped, delivered, cancelled, refunded
    payment_method   VARCHAR(20)    NOT NULL DEFAULT 'cod',  -- cod, vnpay, momo
    total_amount     DECIMAL(15, 2) NOT NULL,
    discount_amount  DECIMAL(15, 2) NOT NULL DEFAULT 0,
    shipping_fee     DECIMAL(15, 2) NOT NULL DEFAULT 0,
    final_amount     DECIMAL(15, 2) NOT NULL,
    promotion_code   VARCHAR(50),
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
    name       VARCHAR(255)   NOT NULL,         -- variant name snapshot
    sku        VARCHAR(100)   NOT NULL,         -- SKU snapshot
    quantity   INT            NOT NULL,
    unit_price DECIMAL(15, 2) NOT NULL,
    subtotal   DECIMAL(15, 2) NOT NULL,
    created_at TIMESTAMP      NOT NULL DEFAULT NOW(),
    CONSTRAINT fk_item_order   FOREIGN KEY (order_id)   REFERENCES orders(id)           ON DELETE CASCADE,
    CONSTRAINT fk_item_variant FOREIGN KEY (variant_id) REFERENCES product_variants(id)
);
```

---

### payments (not implemented)
```sql
CREATE TABLE payment_transactions (
    id              BIGSERIAL      PRIMARY KEY,
    order_id        BIGINT         NOT NULL,
    provider        VARCHAR(20)    NOT NULL,    -- cod, vnpay, momo
    amount          DECIMAL(15, 2) NOT NULL,
    status          VARCHAR(20)    NOT NULL DEFAULT 'pending',
    -- pending, success, failed, refunded
    provider_ref    VARCHAR(255),               -- transaction ID from VNPay/MoMo
    provider_data   JSONB,                      -- raw response from provider
    paid_at         TIMESTAMP,
    created_at      TIMESTAMP      NOT NULL DEFAULT NOW(),
    updated_at      TIMESTAMP      NOT NULL DEFAULT NOW(),
    CONSTRAINT fk_payment_order FOREIGN KEY (order_id) REFERENCES orders(id)
);
```

---

### shipments (not implemented)
```sql
CREATE TABLE shipments (
    id           BIGSERIAL      PRIMARY KEY,
    order_id     BIGINT         NOT NULL UNIQUE,
    provider     VARCHAR(20)    NOT NULL,       -- ghn, ghtk, self
    tracking_code VARCHAR(100),
    status       VARCHAR(30)    NOT NULL DEFAULT 'pending',
    -- pending, picked_up, in_transit, delivered, failed, returned
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
    code                VARCHAR(50)    UNIQUE,          -- NULL = auto sale, value = voucher code
    type                VARCHAR(20)    NOT NULL,        -- percentage, fixed, freeship
    discount_value      DECIMAL(15, 2) NOT NULL,
    min_order_amount    DECIMAL(15, 2) NOT NULL DEFAULT 0,
    max_discount_amount DECIMAL(15, 2),                -- max discount cap, important for percentage
    stackable           BOOLEAN        NOT NULL DEFAULT FALSE,
    priority            INT            NOT NULL DEFAULT 0,
    usage_limit         INT,                           -- NULL = unlimited
    usage_per_user      INT            NOT NULL DEFAULT 1,
    start_at            TIMESTAMP      NOT NULL,
    end_at              TIMESTAMP      NOT NULL,
    status              VARCHAR(20)    NOT NULL DEFAULT 'draft',  -- draft, active, paused, ended
    created_at          TIMESTAMP      NOT NULL DEFAULT NOW(),
    updated_at          TIMESTAMP      NOT NULL DEFAULT NOW()
);

CREATE TABLE promotion_targets (not implemented)
-- This table is designed but has no entity yet
-- Used to apply promotions to specific products/categories/brands

CREATE TABLE promotion_usages (
    id              BIGSERIAL      PRIMARY KEY,
    promotion_id    BIGINT         NOT NULL,
    user_id         BIGINT         NOT NULL,
    order_id        BIGINT         NOT NULL,
    used_code       VARCHAR(50),                       -- store used code if any
    discount_amount DECIMAL(15, 2) NOT NULL,
    created_at      TIMESTAMP      NOT NULL DEFAULT NOW(),
    CONSTRAINT fk_pu_promotion FOREIGN KEY (promotion_id) REFERENCES promotions(id),
    CONSTRAINT fk_pu_user      FOREIGN KEY (user_id)      REFERENCES users(id),
    CONSTRAINT fk_pu_order     FOREIGN KEY (order_id)     REFERENCES orders(id)
);

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
    position   VARCHAR(50)  NOT NULL,           -- home_hero, home_secondary, sidebar
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

## Implemented Tables

The following tables have entities and are operational:

- users, user_tokens
- addresses
- brands
- categories
- products, product_images, product_variants
- reviews
- carts, cart_items
- orders, order_items
- promotions, promotion_usages
- banners
- inventories
- wishlists

## Not Implemented Tables

The following tables are designed in docs but have no entities yet:

- roles, permissions, role_permissions, user_roles
- payment_transactions
- shipments, shipment_logs
- promotion_targets
