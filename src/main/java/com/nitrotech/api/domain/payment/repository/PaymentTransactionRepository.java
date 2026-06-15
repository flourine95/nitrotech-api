package com.nitrotech.api.domain.payment.repository;

import com.nitrotech.api.domain.payment.dto.VerifiedPaymentWebhook;

public interface PaymentTransactionRepository {
    boolean existsByProviderAndProviderRef(String provider, String providerRef);
    void save(VerifiedPaymentWebhook verified, String status);
}
