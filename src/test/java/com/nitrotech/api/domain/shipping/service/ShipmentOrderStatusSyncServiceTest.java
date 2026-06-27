package com.nitrotech.api.domain.shipping.service;

import com.nitrotech.api.domain.order.dto.OrderData;
import com.nitrotech.api.domain.order.repository.OrderRepository;
import com.nitrotech.api.domain.order.usecase.UpdateOrderStatusUseCase;
import com.nitrotech.api.domain.shipping.dto.ShipmentData;
import com.nitrotech.api.domain.shipping.ShipmentStatus;
import org.junit.jupiter.api.Test;

import java.util.Optional;

import static org.mockito.Mockito.*;

class ShipmentOrderStatusSyncServiceTest {

    @Test
    void deliveredShipmentUsesOrderTransitionUseCaseForEveryRequiredStep() {
        OrderRepository orderRepository = mock(OrderRepository.class);
        UpdateOrderStatusUseCase updateOrderStatusUseCase = mock(UpdateOrderStatusUseCase.class);
        ShipmentOrderStatusSyncService service = new ShipmentOrderStatusSyncService(
                orderRepository,
                updateOrderStatusUseCase
        );
        OrderData order = mock(OrderData.class);
        when(order.id()).thenReturn(123L);
        when(order.status()).thenReturn("processing");
        when(orderRepository.findById(123L)).thenReturn(Optional.of(order));

        service.syncDeliveredShipment(ShipmentData.builder()
                .orderId(123L)
                .status(ShipmentStatus.DELIVERED)
                .build(), "GHTK status_id=5");

        verify(updateOrderStatusUseCase).execute(123L, "shipped", "shipping_webhook", "GHTK status_id=5");
        verify(updateOrderStatusUseCase).execute(123L, "delivered", "shipping_webhook", "GHTK status_id=5");
        verify(orderRepository, never()).updateStatus(anyLong(), anyString());
    }
}
