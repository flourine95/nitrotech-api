package com.nitrotech.api.infrastructure.shipping;

import com.nitrotech.api.domain.shipping.provider.ShippingProvider;
import com.nitrotech.api.shared.exception.BadRequestException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class ShippingProviderRegistryTest {

    private ShippingProvider ghtkProvider;
    private ShippingProvider ghnProvider;
    private ShippingProviderRegistry registry;

    @BeforeEach
    void setUp() {
        ghtkProvider = mock(ShippingProvider.class);
        when(ghtkProvider.getProviderName()).thenReturn("ghtk");

        ghnProvider = mock(ShippingProvider.class);
        when(ghnProvider.getProviderName()).thenReturn("ghn");

        registry = new ShippingProviderRegistry(List.of(ghtkProvider, ghnProvider));
    }

    @Test
    void retrievesProviderByNameCaseInsensitively() {
        assertThat(registry.getProvider("ghtk")).isEqualTo(ghtkProvider);
        assertThat(registry.getProvider("GHTK")).isEqualTo(ghtkProvider);
        assertThat(registry.getProvider("ghn")).isEqualTo(ghnProvider);
        assertThat(registry.getProvider("Ghn")).isEqualTo(ghnProvider);
    }

    @Test
    void throwsExceptionWhenProviderNotFound() {
        assertThatThrownBy(() -> registry.getProvider("viettelpost"))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("Shipping provider 'viettelpost' is not supported");
    }
}
