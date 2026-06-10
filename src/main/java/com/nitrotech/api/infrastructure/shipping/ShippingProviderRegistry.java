package com.nitrotech.api.infrastructure.shipping;

import com.nitrotech.api.domain.shipping.provider.ShippingProvider;
import com.nitrotech.api.shared.exception.BadRequestException;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;
import java.util.stream.Collectors;

@Component
public class ShippingProviderRegistry {

    private final Map<String, ShippingProvider> providers;

    public ShippingProviderRegistry(List<ShippingProvider> providerList) {
        this.providers = providerList.stream()
                .collect(Collectors.toMap(
                        provider -> provider.getProviderName().toLowerCase(),
                        Function.identity()
                ));
    }

    public ShippingProvider getProvider(String name) {
        return Optional.ofNullable(name)
                .map(String::toLowerCase)
                .map(providers::get)
                .orElseThrow(() -> new BadRequestException("INVALID_SHIPPING_PROVIDER",
                        "Shipping provider '" + name + "' is not supported."));
    }
}
