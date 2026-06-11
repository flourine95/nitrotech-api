package com.nitrotech.api.domain.shipping.usecase;

import com.nitrotech.api.domain.order.dto.OrderData;
import com.nitrotech.api.domain.order.repository.OrderRepository;
import com.nitrotech.api.domain.shipping.dto.ShipmentData;
import com.nitrotech.api.domain.shipping.dto.ShippingResult;
import com.nitrotech.api.domain.shipping.provider.ShippingProvider;
import com.nitrotech.api.domain.shipping.repository.ShipmentRepository;
import com.nitrotech.api.infrastructure.shipping.ShippingProviderRegistry;
import com.nitrotech.api.shared.exception.NotFoundException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.*;

class CreateShipmentUseCaseTest {

    private OrderRepository orderRepository;
    private ShipmentRepository shipmentRepository;
    private ShippingProviderRegistry registry;
    private CreateShipmentUseCase useCase;

    @BeforeEach
    void setUp() {
        orderRepository = mock(OrderRepository.class);
        shipmentRepository = mock(ShipmentRepository.class);
        registry = mock(ShippingProviderRegistry.class);
        useCase = new CreateShipmentUseCase(orderRepository, shipmentRepository, registry, "ghtk");
    }

    @Test
    void createsShipmentSuccessfullyAndLogsStatus() {
        Long orderId = 123L;
        String providerName = "ghtk";

        OrderData order = mock(OrderData.class);
        when(orderRepository.findById(orderId)).thenReturn(Optional.of(order));
        when(shipmentRepository.findByOrderId(orderId)).thenReturn(Optional.empty());

        ShippingProvider provider = mock(ShippingProvider.class);
        when(provider.getProviderName()).thenReturn("ghtk");
        when(registry.getProvider(providerName)).thenReturn(provider);

        Instant estTime = Instant.now().plusSeconds(86400 * 2);
        ShippingResult shippingResult = ShippingResult.builder()
                .trackingCode("S12345.6789")
                .fee(new BigDecimal("35000"))
                .estimatedAt(estTime)
                .build();
        when(provider.createShipment(order)).thenReturn(shippingResult);

        ShipmentData savedMock = ShipmentData.builder()
                .id(1L)
                .orderId(orderId)
                .provider(providerName)
                .trackingCode("S12345.6789")
                .status("ready_to_pick")
                .fee(new BigDecimal("35000"))
                .estimatedAt(estTime)
                .build();
        when(shipmentRepository.save(any(ShipmentData.class))).thenReturn(savedMock);

        ShipmentData result = useCase.execute(orderId, providerName);

        assertThat(result).isNotNull();
        assertThat(result.getId()).isEqualTo(1L);
        assertThat(result.getTrackingCode()).isEqualTo("S12345.6789");

        ArgumentCaptor<ShipmentData> shipmentCaptor = ArgumentCaptor.forClass(ShipmentData.class);
        verify(shipmentRepository).save(shipmentCaptor.capture());
        ShipmentData captured = shipmentCaptor.getValue();
        assertThat(captured.getOrderId()).isEqualTo(orderId);
        assertThat(captured.getProvider()).isEqualTo(providerName);
        assertThat(captured.getTrackingCode()).isEqualTo("S12345.6789");
        assertThat(captured.getStatus()).isEqualTo("ready_to_pick");

        verify(shipmentRepository).addLog(eq(1L), eq("ready_to_pick"), isNull(), anyString());
    }

    @Test
    void returnsExistingShipmentWithoutRecreating() {
        Long orderId = 123L;
        ShipmentData existing = ShipmentData.builder()
                .id(1L)
                .orderId(orderId)
                .provider("ghtk")
                .trackingCode("S12345.6789")
                .status("ready_to_pick")
                .build();
        when(shipmentRepository.findByOrderId(orderId)).thenReturn(Optional.of(existing));

        ShipmentData result = useCase.execute(orderId, "ghtk");

        assertThat(result).isEqualTo(existing);
        verify(orderRepository, never()).findById(anyLong());
        verify(registry, never()).getProvider(anyString());
        verify(shipmentRepository, never()).save(any());
    }

    @Test
    void throwsNotFoundExceptionWhenOrderDoesNotExist() {
        Long orderId = 123L;
        when(shipmentRepository.findByOrderId(orderId)).thenReturn(Optional.empty());
        when(orderRepository.findById(orderId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> useCase.execute(orderId, "ghtk"))
                .isInstanceOf(NotFoundException.class)
                .hasMessageContaining("Order with ID 123 not found");

        verify(registry, never()).getProvider(anyString());
    }
}
