package com.nitrotech.api.domain.order.usecase;

import com.nitrotech.api.domain.inventory.repository.InventoryRepository;
import com.nitrotech.api.domain.order.dto.OrderData;
import com.nitrotech.api.domain.order.dto.OrderItemData;
import com.nitrotech.api.domain.order.repository.OrderRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

class ExpirePendingOrdersUseCaseTest {

    private OrderRepository orderRepository;
    private InventoryRepository inventoryRepository;
    private ExpirePendingOrdersUseCase useCase;

    @BeforeEach
    void setUp() {
        orderRepository = mock(OrderRepository.class);
        inventoryRepository = mock(InventoryRepository.class);
        Clock clock = Clock.fixed(Instant.parse("2026-06-10T10:30:00Z"), ZoneOffset.UTC);
        useCase = new ExpirePendingOrdersUseCase(orderRepository, inventoryRepository, clock);
    }

    @Test
    void expiresPendingOrdersCreatedBeforePaymentTimeout() {
        when(orderRepository.findPendingCreatedAtOrBefore(Instant.parse("2026-06-10T10:15:00Z")))
                .thenReturn(List.of(order()));

        int expired = useCase.execute();

        assertThat(expired).isEqualTo(1);
        verify(orderRepository).updateStatus(123L, "expired");
        verify(inventoryRepository).adjust(101L, 2);
    }

    private OrderData order() {
        return new OrderData(
                123L, 10L, "SO-123", null, "pending", "sepay",
                new BigDecimal("500000"), BigDecimal.ZERO, BigDecimal.ZERO, new BigDecimal("500000"),
                null, null,
                List.of(new OrderItemData(1L, 101L, "RTX 4060", "SKU-101", 2,
                        new BigDecimal("250000"), new BigDecimal("500000"), null,
                        1000, null, null, null)),
                Instant.now(), Instant.now(), null, null
        );
    }
}
