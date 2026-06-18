package com.nitrotech.api.domain.order.usecase;

import com.nitrotech.api.domain.order.dto.OrderData;
import com.nitrotech.api.domain.order.repository.OrderRepository;
import com.nitrotech.api.shared.exception.DomainException;
import com.nitrotech.api.shared.exception.NotFoundException;
import com.nitrotech.api.domain.audit.service.AuditLogService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.*;

class CancelOrderUseCaseTest {

    private OrderRepository orderRepository;
    private AuditLogService auditLogService;
    private CancelOrderUseCase useCase;

    @BeforeEach
    void setUp() {
        orderRepository = mock(OrderRepository.class);
        auditLogService = mock(AuditLogService.class);
        useCase = new CancelOrderUseCase(orderRepository, auditLogService);
    }

    @Test
    void cancelsPendingOrderOwnedByUser() {
        when(orderRepository.findByIdAndUserId(123L, 10L)).thenReturn(Optional.of(order("pending")));
        when(orderRepository.updateStatus(123L, "cancelled")).thenReturn(order("cancelled"));

        OrderData result = useCase.execute(123L, 10L);

        assertThat(result.status()).isEqualTo("cancelled");
        verify(orderRepository).updateStatus(123L, "cancelled");
        verify(auditLogService).record(any());
    }

    @Test
    void cancelsConfirmedOrderOwnedByUser() {
        when(orderRepository.findByIdAndUserId(123L, 10L)).thenReturn(Optional.of(order("confirmed")));
        when(orderRepository.updateStatus(123L, "cancelled")).thenReturn(order("cancelled"));

        OrderData result = useCase.execute(123L, 10L);

        assertThat(result.status()).isEqualTo("cancelled");
        verify(orderRepository).updateStatus(123L, "cancelled");
        verify(auditLogService).record(any());
    }

    @Test
    void rejectsCancelWhenOrderIsNotOwnedByUserOrMissing() {
        when(orderRepository.findByIdAndUserId(123L, 10L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> useCase.execute(123L, 10L))
                .isInstanceOf(NotFoundException.class)
                .hasMessage("Order not found");

        verify(orderRepository, never()).updateStatus(anyLong(), anyString());
    }

    @Test
    void rejectsCancelWhenOrderStatusIsNotCancellable() {
        when(orderRepository.findByIdAndUserId(123L, 10L)).thenReturn(Optional.of(order("shipped")));

        assertThatThrownBy(() -> useCase.execute(123L, 10L))
                .isInstanceOf(DomainException.class)
                .hasMessage("Order cannot be cancelled in status: shipped");

        verify(orderRepository, never()).updateStatus(anyLong(), anyString());
    }

    private OrderData order(String status) {
        return new OrderData(
                123L,
                10L,
                "SO-123",
                null,
                status,
                "sepay",
                new BigDecimal("500000"),
                BigDecimal.ZERO,
                BigDecimal.ZERO,
                new BigDecimal("500000"),
                null,
                null,
                List.of(),
                Instant.now(),
                Instant.now()
        );
    }
}
