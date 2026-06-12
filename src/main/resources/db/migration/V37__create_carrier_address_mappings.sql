CREATE TABLE carrier_address_mappings (
    id                     BIGSERIAL PRIMARY KEY,
    provider               VARCHAR(30)  NOT NULL,
    province_code          VARCHAR(50)  NOT NULL,
    district_code          VARCHAR(50)  NOT NULL,
    ward_code              VARCHAR(50)  NOT NULL,
    province_name          VARCHAR(255) NOT NULL,
    district_name          VARCHAR(255) NOT NULL,
    ward_name              VARCHAR(255) NOT NULL,
    carrier_province_id    VARCHAR(50),
    carrier_district_id    VARCHAR(50),
    carrier_ward_code      VARCHAR(50),
    carrier_province_name  VARCHAR(255),
    carrier_district_name  VARCHAR(255),
    carrier_ward_name      VARCHAR(255),
    confidence             VARCHAR(20)  NOT NULL DEFAULT 'verified',
    active                 BOOLEAN      NOT NULL DEFAULT true,
    created_at             TIMESTAMP    NOT NULL DEFAULT NOW(),
    updated_at             TIMESTAMP    NOT NULL DEFAULT NOW(),
    CONSTRAINT uq_carrier_address_mapping UNIQUE (provider, province_code, district_code, ward_code)
);

CREATE INDEX idx_carrier_address_mappings_provider_active
    ON carrier_address_mappings(provider, active);
