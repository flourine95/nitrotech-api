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
