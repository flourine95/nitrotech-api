CREATE TABLE promotions (
    id                  BIGSERIAL      PRIMARY KEY,
    name                VARCHAR(255)   NOT NULL,
    description         TEXT,
    code                VARCHAR(50)    UNIQUE,
    type                VARCHAR(20)    NOT NULL,
    discount_value      DECIMAL(15, 2) NOT NULL,
    min_order_amount    DECIMAL(15, 2) NOT NULL DEFAULT 0,
    max_discount_amount DECIMAL(15, 2),
    stackable           BOOLEAN        NOT NULL DEFAULT FALSE,
    priority            INT            NOT NULL DEFAULT 0,
    usage_limit         INT,
    usage_per_user      INT            NOT NULL DEFAULT 1,
    start_at            TIMESTAMP      NOT NULL,
    end_at              TIMESTAMP      NOT NULL,
    status              VARCHAR(20)    NOT NULL DEFAULT 'draft',
    created_at          TIMESTAMP      NOT NULL DEFAULT NOW(),
    updated_at          TIMESTAMP      NOT NULL DEFAULT NOW()
);

CREATE TABLE promotion_targets (
    id           BIGSERIAL   PRIMARY KEY,
    promotion_id BIGINT      NOT NULL,
    target_type  VARCHAR(20) NOT NULL,
    target_id    BIGINT      NOT NULL,
    excluded     BOOLEAN     NOT NULL DEFAULT FALSE,
    CONSTRAINT fk_pt_promotion FOREIGN KEY (promotion_id) REFERENCES promotions(id) ON DELETE CASCADE
);

CREATE INDEX idx_pt_lookup ON promotion_targets (promotion_id, target_type, target_id);

CREATE TABLE promotion_usages (
    id              BIGSERIAL      PRIMARY KEY,
    promotion_id    BIGINT         NOT NULL,
    user_id         BIGINT         NOT NULL,
    order_id        BIGINT         NOT NULL,
    used_code       VARCHAR(50),
    discount_amount DECIMAL(15, 2) NOT NULL,
    created_at      TIMESTAMP      NOT NULL DEFAULT NOW(),
    CONSTRAINT fk_pu_promotion FOREIGN KEY (promotion_id) REFERENCES promotions(id),
    CONSTRAINT fk_pu_user      FOREIGN KEY (user_id)      REFERENCES users(id),
    CONSTRAINT fk_pu_order     FOREIGN KEY (order_id)     REFERENCES orders(id)
);

CREATE INDEX idx_pu_lookup ON promotion_usages (promotion_id, user_id);
