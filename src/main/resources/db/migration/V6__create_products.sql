CREATE TABLE products (
    id          BIGSERIAL    PRIMARY KEY,
    category_id BIGINT       NOT NULL,
    brand_id    BIGINT,
    name        VARCHAR(255) NOT NULL,
    slug        VARCHAR(255) NOT NULL UNIQUE,
    description TEXT,
    thumbnail   VARCHAR(500),
    specs       JSONB,
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
    name       VARCHAR(255)   NOT NULL,
    price      DECIMAL(15, 2) NOT NULL,
    attributes JSONB,
    active     BOOLEAN        NOT NULL DEFAULT TRUE,
    created_at TIMESTAMP      NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMP      NOT NULL DEFAULT NOW(),
    deleted_at TIMESTAMP,
    CONSTRAINT fk_variant_product FOREIGN KEY (product_id) REFERENCES products(id) ON DELETE CASCADE
);

CREATE INDEX idx_variant_attributes ON product_variants USING GIN (attributes);
