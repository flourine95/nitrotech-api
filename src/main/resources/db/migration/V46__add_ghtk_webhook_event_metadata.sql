ALTER TABLE shipments
    ADD COLUMN last_official_event_at TIMESTAMPTZ;

ALTER TABLE shipment_logs
    ADD COLUMN occurred_at TIMESTAMPTZ,
    ADD COLUMN reason_code VARCHAR(100),
    ADD COLUMN raw_payload JSONB,
    ADD COLUMN event_key VARCHAR(128);

CREATE UNIQUE INDEX uq_shipment_logs_event_key
    ON shipment_logs (event_key)
    WHERE event_key IS NOT NULL;
