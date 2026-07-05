CREATE TABLE oauth_accounts (
    id           BIGSERIAL PRIMARY KEY,
    user_id      BIGINT       NOT NULL,
    provider     VARCHAR(50)  NOT NULL,
    external_id  VARCHAR(255) NOT NULL,
    email        VARCHAR(255) NOT NULL,
    display_name VARCHAR(255),
    avatar_url   VARCHAR(500),
    created_at   TIMESTAMPTZ  NOT NULL DEFAULT NOW(),
    updated_at   TIMESTAMPTZ  NOT NULL DEFAULT NOW(),
    CONSTRAINT fk_oauth_account_user FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE,
    CONSTRAINT uq_oauth_accounts_provider_external_id UNIQUE (provider, external_id),
    CONSTRAINT uq_oauth_accounts_provider_user_id UNIQUE (provider, user_id)
);
