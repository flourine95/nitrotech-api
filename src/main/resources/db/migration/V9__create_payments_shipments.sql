CREATE TABLE payment_transactions (
    id            BIGSERIAL      PRIMARY KEY,
    order_id      BIGINT         NOT NULL,
    provider      VARCHAR(20)    NOT NULL,
    amount        DECIMAL(15, 2) NOT NULL,
    status        VARCHAR(20)    NOT NULL DEFAULT 'pending',
    provider_ref  VARCHAR(255),
    provider_data JSONB,
    paid_at       TIMESTAMP,
    created_at    TIMESTAMP      NOT NULL DEFAULT NOW(),
    updated_at    TIMESTAMP      NOT NULL DEFAULT NOW(),
    CONSTRAINT fk_payment_order FOREIGN KEY (order_id) REFERENCES orders(id)
);

CREATE TABLE shipments (
    id            BIGSERIAL      PRIMARY KEY,
    order_id      BIGINT         NOT NULL UNIQUE,
    provider      VARCHAR(20)    NOT NULL,
    tracking_code VARCHAR(100),
    status        VARCHAR(30)    NOT NULL DEFAULT 'pending',
    fee           DECIMAL(15, 2) NOT NULL DEFAULT 0,
    estimated_at  TIMESTAMP,
    shipped_at    TIMESTAMP,
    delivered_at  TIMESTAMP,
    created_at    TIMESTAMP      NOT NULL DEFAULT NOW(),
    updated_at    TIMESTAMP      NOT NULL DEFAULT NOW(),
    CONSTRAINT fk_shipment_order FOREIGN KEY (order_id) REFERENCES orders(id)
);

CREATE TABLE shipment_logs (
    id          BIGSERIAL   PRIMARY KEY,
    shipment_id BIGINT      NOT NULL,
    status      VARCHAR(30) NOT NULL,
    location    VARCHAR(255),
    note        TEXT,
    created_at  TIMESTAMP   NOT NULL DEFAULT NOW(),
    CONSTRAINT fk_log_shipment FOREIGN KEY (shipment_id) REFERENCES shipments(id) ON DELETE CASCADE
);
