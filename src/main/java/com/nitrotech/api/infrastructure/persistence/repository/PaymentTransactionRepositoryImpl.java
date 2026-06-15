package com.nitrotech.api.infrastructure.persistence.repository;

import com.nitrotech.api.domain.payment.dto.VerifiedPaymentWebhook;
import com.nitrotech.api.domain.payment.repository.PaymentTransactionRepository;
import com.nitrotech.api.infrastructure.persistence.entity.PaymentTransactionEntity;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.time.Instant;

@Repository
@RequiredArgsConstructor
public class PaymentTransactionRepositoryImpl implements PaymentTransactionRepository {

    private final PaymentTransactionJpaRepository jpa;

    @Override
    public boolean existsByProviderAndProviderRef(String provider, String providerRef) {
        return jpa.findByProviderAndProviderRef(provider, providerRef).isPresent();
    }

    @Override
    public void save(VerifiedPaymentWebhook verified, String status) {
        PaymentTransactionEntity entity = new PaymentTransactionEntity();
        entity.setOrderId(verified.orderId());
        entity.setProvider(verified.provider());
        entity.setAmount(verified.amount() != null ? verified.amount() : BigDecimal.ZERO);
        entity.setStatus(status);
        entity.setProviderRef(verified.externalTransactionId());
        entity.setProviderData(verified.rawData());
        entity.setPaidAt("paid".equals(status) ? Instant.now() : null);
        jpa.save(entity);
    }
}
