package com.nitrotech.api.domain.payment.provider;

public interface PaymentProviderResolver {
    PaymentProvider getProvider(String name);
}
