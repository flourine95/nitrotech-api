package com.nitrotech.api.domain.payment.provider;

import com.nitrotech.api.shared.exception.BadRequestException;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class PaymentProviderResolver {

    private final List<PaymentProvider> providers;

    public PaymentProviderResolver(List<PaymentProvider> providers) {
        this.providers = providers;
    }

    public PaymentProvider getProvider(String providerName) {
        return providers.stream()
                .filter(provider -> provider.getProviderName().equalsIgnoreCase(providerName))
                .findFirst()
                .orElseThrow(() -> new BadRequestException(
                        "PAYMENT_METHOD_UNSUPPORTED",
                        "Payment method is not supported yet: " + providerName
                ));
    }
}