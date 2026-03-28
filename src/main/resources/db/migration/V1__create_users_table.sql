CREATE TABLE users (
    id                BIGSERIAL PRIMARY KEY,
    name              VARCHAR(255) NOT NULL,
    email             VARCHAR(255) NOT NULL UNIQUE,
    password          VARCHAR(255),
    phone             VARCHAR(20),
    avatar            VARCHAR(500),
    status            VARCHAR(20)  NOT NULL DEFAULT 'inactive',
    provider          VARCHAR(20)  NOT NULL DEFAULT 'local',
    provider_id       VARCHAR(100),
    created_at        TIMESTAMP    NOT NULL DEFAULT NOW(),
    updated_at        TIMESTAMP    NOT NULL DEFAULT NOW(),
    deleted_at        TIMESTAMP
);
