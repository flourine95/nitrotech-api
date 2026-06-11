package com.nitrotech.api.infrastructure.payment;

import com.nitrotech.api.domain.payment.provider.PaymentProvider;
import com.nitrotech.api.shared.exception.BadRequestException;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;
import java.util.stream.Collectors;

@Component
public class PaymentProviderRegistry {

    private final Map<String, PaymentProvider> providers;

    public PaymentProviderRegistry(List<PaymentProvider> providerList) {
        this.providers = providerList.stream()
                .collect(Collectors.toMap(
                        provider -> provider.getProviderName().toLowerCase(),
                        Function.identity()
                ));
    }

    public PaymentProvider getProvider(String name) {
        return Optional.ofNullable(name)
                .map(String::toLowerCase)
                .map(providers::get)
                .orElseThrow(() -> new BadRequestException("INVALID_PAYMENT_METHOD",
                        "Payment method '" + name + "' is not supported."));
    }
}
