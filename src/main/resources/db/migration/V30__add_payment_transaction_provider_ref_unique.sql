CREATE UNIQUE INDEX uq_payment_transactions_provider_ref
    ON payment_transactions (provider, provider_ref)
    WHERE provider_ref IS NOT NULL;
