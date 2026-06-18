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

class UpdateOrderStatusUseCaseTest {

    private OrderRepository orderRepository;
    private AuditLogService auditLogService;
    private UpdateOrderStatusUseCase useCase;

    @BeforeEach
    void setUp() {
        orderRepository = mock(OrderRepository.class);
        auditLogService = mock(AuditLogService.class);
        useCase = new UpdateOrderStatusUseCase(orderRepository, auditLogService);
    }

    @Test
    void updatesStatusWhenTransitionIsAllowed() {
        when(orderRepository.findById(123L)).thenReturn(Optional.of(order("pending")));
        when(orderRepository.updateStatus(123L, "confirmed")).thenReturn(order("confirmed"));

        OrderData result = useCase.execute(123L, "confirmed");

        assertThat(result.status()).isEqualTo("confirmed");
        verify(orderRepository).updateStatus(123L, "confirmed");
        verify(auditLogService).record(any());
    }

    @Test
    void allowsForwardFulfillmentTransitions() {
        when(orderRepository.findById(123L)).thenReturn(Optional.of(order("processing")));
        when(orderRepository.updateStatus(123L, "shipped")).thenReturn(order("shipped"));

        OrderData result = useCase.execute(123L, "shipped");

        assertThat(result.status()).isEqualTo("shipped");
        verify(orderRepository).updateStatus(123L, "shipped");
        verify(auditLogService).record(any());
    }

    @Test
    void rejectsInvalidTransition() {
        when(orderRepository.findById(123L)).thenReturn(Optional.of(order("pending")));

        assertThatThrownBy(() -> useCase.execute(123L, "shipped"))
                .isInstanceOf(DomainException.class)
                .hasMessage("Cannot transition from pending to shipped");

        verify(orderRepository, never()).updateStatus(anyLong(), anyString());
    }

    @Test
    void rejectsTransitionFromTerminalStatusWithoutAllowedNextStatus() {
        when(orderRepository.findById(123L)).thenReturn(Optional.of(order("cancelled")));

        assertThatThrownBy(() -> useCase.execute(123L, "confirmed"))
                .isInstanceOf(DomainException.class)
                .hasMessage("Cannot transition from cancelled to confirmed");

        verify(orderRepository, never()).updateStatus(anyLong(), anyString());
    }

    @Test
    void rejectsUpdateWhenOrderCannotBeFound() {
        when(orderRepository.findById(123L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> useCase.execute(123L, "confirmed"))
                .isInstanceOf(NotFoundException.class)
                .hasMessage("Order not found");

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
