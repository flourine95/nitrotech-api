package com.nitrotech.api.domain.shipping.usecase;

import com.nitrotech.api.domain.order.dto.OrderData;
import com.nitrotech.api.domain.order.repository.OrderRepository;
import com.nitrotech.api.domain.shipping.dto.ShipmentData;
import com.nitrotech.api.domain.shipping.repository.ShipmentRepository;
import com.nitrotech.api.shared.exception.BadRequestException;
import com.nitrotech.api.shared.exception.NotFoundException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Map;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

class HandleShippingWebhookUseCaseTest {

    private ShipmentRepository shipmentRepository;
    private OrderRepository orderRepository;
    private HandleShippingWebhookUseCase useCase;

    @BeforeEach
    void setUp() {
        shipmentRepository = mock(ShipmentRepository.class);
        orderRepository = mock(OrderRepository.class);
        useCase = new HandleShippingWebhookUseCase(shipmentRepository, orderRepository);
    }

    @Test
    void updatesShipmentStatusAndAddsLog() {
        ShipmentData shipment = ShipmentData.builder()
                .id(10L)
                .orderId(123L)
                .provider("ghn")
                .trackingCode("GHN123")
                .status("ready_to_pick")
                .build();

        when(shipmentRepository.findByProviderAndTrackingCode("ghn", "GHN123"))
                .thenReturn(Optional.of(shipment));
        when(shipmentRepository.save(any(ShipmentData.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        Map<String, Object> result = useCase.execute("ghn", Map.of(
                "OrderCode", "GHN123",
                "Status", "delivered",
                "Warehouse", "HCM",
                "Type", "Switch_status"
        ));

        assertThat(result.get("ok")).isEqualTo(true);
        assertThat(result.get("status")).isEqualTo("delivered");
        assertThat(shipment.getStatus()).isEqualTo("delivered");
        assertThat(shipment.getDeliveredAt()).isNotNull();

        verify(shipmentRepository).save(shipment);
        verify(shipmentRepository).addLog(10L, "delivered", "delivered", "WEBHOOK",
                "HCM", "Webhook GHN: delivered (Switch_status)");
    }

    @Test
    void mapsGhnDeliveryStatusAliases() {
        ShipmentData shipment = ShipmentData.builder()
                .id(10L)
                .provider("ghn")
                .trackingCode("GHN123")
                .status("ready_to_pick")
                .build();

        when(shipmentRepository.findByProviderAndTrackingCode("ghn", "GHN123"))
                .thenReturn(Optional.of(shipment));
        when(shipmentRepository.save(any(ShipmentData.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        useCase.execute("ghn", Map.of(
                "order_code", "GHN123",
                "status", "delivering"
        ));

        assertThat(shipment.getStatus()).isEqualTo("delivering");
        assertThat(shipment.getShippedAt()).isNotNull();
    }

    @Test
    void keepsDocumentedGhnStatusInsteadOfCollapsingIt() {
        ShipmentData shipment = ShipmentData.builder()
                .id(10L)
                .provider("ghn")
                .trackingCode("GHN123")
                .status("ready_to_pick")
                .build();

        when(shipmentRepository.findByProviderAndTrackingCode("ghn", "GHN123"))
                .thenReturn(Optional.of(shipment));
        when(shipmentRepository.save(any(ShipmentData.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        useCase.execute("ghn", Map.of(
                "OrderCode", "GHN123",
                "Status", "return_transporting",
                "Warehouse", "Buu cuc GHN"
        ));

        assertThat(shipment.getStatus()).isEqualTo("return_transporting");
        assertThat(shipment.getShippedAt()).isNotNull();
        verify(shipmentRepository).addLog(10L, "return_transporting", "return_transporting", "WEBHOOK", "Buu cuc GHN",
                "Webhook GHN: return_transporting");
    }

    @Test
    void handlesGhtkWebhookPayload() {
        ShipmentData shipment = ShipmentData.builder()
                .id(20L)
                .provider("ghtk")
                .trackingCode("S1.A1.17373471")
                .status("ready_to_pick")
                .build();

        when(shipmentRepository.findByProviderAndTrackingCode("ghtk", "S1.A1.17373471"))
                .thenReturn(Optional.of(shipment));
        when(shipmentRepository.save(any(ShipmentData.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        Map<String, Object> result = useCase.execute("GHTK", Map.of(
                "partner_id", "1234567",
                "label_id", "S1.A1.17373471",
                "status_id", 5,
                "action_time", "2016-11-02T12:18:39+07:00",
                "reason", ""
        ));

        assertThat(result.get("ok")).isEqualTo(true);
        assertThat(result.get("status")).isEqualTo("delivered");
        assertThat(shipment.getDeliveredAt()).isNotNull();
        verify(shipmentRepository).addLog(20L, "delivered", "5", "WEBHOOK", null,
                "Webhook GHTK: 5 (status_5)");
    }

    @Test
    void deliveredWebhookCompletesOrder() {
        ShipmentData shipment = ShipmentData.builder()
                .id(20L)
                .orderId(123L)
                .provider("ghtk")
                .trackingCode("S1.A1.17373471")
                .status("ready_to_pick")
                .build();
        OrderData order = mock(OrderData.class);
        when(order.id()).thenReturn(123L);
        when(order.status()).thenReturn("processing");

        when(shipmentRepository.findByProviderAndTrackingCode("ghtk", "S1.A1.17373471"))
                .thenReturn(Optional.of(shipment));
        when(shipmentRepository.save(any(ShipmentData.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));
        when(orderRepository.findById(123L)).thenReturn(Optional.of(order));

        useCase.execute("ghtk", Map.of(
                "label_id", "S1.A1.17373471",
                "status_id", 5
        ));

        verify(orderRepository).updateStatus(123L, "delivered");
    }

    @Test
    void throwsWhenTrackingCodeOrStatusIsMissing() {
        assertThatThrownBy(() -> useCase.execute("ghn", Map.of("OrderCode", "GHN123")))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("tracking code and status");

        verify(shipmentRepository, never()).save(any());
    }

    @Test
    void throwsWhenShipmentCannotBeFound() {
        when(shipmentRepository.findByProviderAndTrackingCode("ghn", "GHN123"))
                .thenReturn(Optional.empty());

        assertThatThrownBy(() -> useCase.execute("ghn", Map.of(
                "OrderCode", "GHN123",
                "Status", "delivered"
        )))
                .isInstanceOf(NotFoundException.class)
                .hasMessageContaining("GHN123");
    }
}
