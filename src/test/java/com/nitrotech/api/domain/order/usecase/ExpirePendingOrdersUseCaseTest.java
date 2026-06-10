package com.nitrotech.api.domain.order.usecase;

import com.nitrotech.api.domain.order.repository.OrderRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

class ExpirePendingOrdersUseCaseTest {

    private OrderRepository orderRepository;
    private ExpirePendingOrdersUseCase useCase;

    @BeforeEach
    void setUp() {
        orderRepository = mock(OrderRepository.class);
        Clock clock = Clock.fixed(Instant.parse("2026-06-10T10:30:00Z"), ZoneOffset.UTC);
        useCase = new ExpirePendingOrdersUseCase(orderRepository, clock);
    }

    @Test
    void expiresPendingOrdersCreatedBeforePaymentTimeout() {
        when(orderRepository.expirePendingCreatedAtOrBefore(
                Instant.parse("2026-06-10T10:15:00Z"),
                Instant.parse("2026-06-10T10:30:00Z")))
                .thenReturn(3);

        int expired = useCase.execute();

        assertThat(expired).isEqualTo(3);
        verify(orderRepository).expirePendingCreatedAtOrBefore(
                Instant.parse("2026-06-10T10:15:00Z"),
                Instant.parse("2026-06-10T10:30:00Z"));
    }
}
