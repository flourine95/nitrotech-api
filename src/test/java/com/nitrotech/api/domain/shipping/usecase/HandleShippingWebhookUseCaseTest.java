package com.nitrotech.api.domain.shipping.usecase;

import com.nitrotech.api.domain.order.dto.OrderData;
import com.nitrotech.api.domain.order.repository.OrderRepository;
import com.nitrotech.api.domain.audit.dto.AuditLogCommand;
import com.nitrotech.api.domain.audit.service.AuditLogService;
import com.nitrotech.api.domain.shipping.dto.ShipmentData;
import com.nitrotech.api.domain.shipping.dto.ShipmentLogSource;
import com.nitrotech.api.domain.shipping.ShipmentStatus;
import com.nitrotech.api.domain.shipping.repository.ShipmentRepository;
import com.nitrotech.api.domain.shipping.service.ShipmentOrderStatusSyncService;
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
    private AuditLogService auditLogService;
    private ShipmentOrderStatusSyncService shipmentOrderStatusSyncService;
    private GhtkWebhookProcessor ghtkWebhookProcessor;
    private HandleShippingWebhookUseCase useCase;

    @BeforeEach
    void setUp() {
        shipmentRepository = mock(ShipmentRepository.class);
        auditLogService = mock(AuditLogService.class);
        shipmentOrderStatusSyncService = mock(ShipmentOrderStatusSyncService.class);
        ghtkWebhookProcessor = mock(GhtkWebhookProcessor.class);
        useCase = new HandleShippingWebhookUseCase(
                shipmentRepository,
                auditLogService,
                shipmentOrderStatusSyncService,
                ghtkWebhookProcessor
        );
    }

    @Test
    void updatesShipmentStatusAndAddsLog() {
        ShipmentData shipment = ShipmentData.builder()
                .id(10L)
                .orderId(123L)
                .provider("ghn")
                .trackingCode("GHN123")
                .status(ShipmentStatus.READY_TO_PICK)
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
        assertThat(shipment.getStatus()).isEqualTo(ShipmentStatus.DELIVERED);
        assertThat(shipment.getDeliveredAt()).isNotNull();

        verify(shipmentRepository).save(shipment);
        verify(shipmentRepository).addLog(10L, ShipmentStatus.DELIVERED, "delivered", ShipmentLogSource.WEBHOOK,
                "HCM", "Webhook GHN: delivered (Switch_status)");
        verify(auditLogService).record(any(AuditLogCommand.class));
    }

    @Test
    void mapsGhnDeliveryStatusAliases() {
        ShipmentData shipment = ShipmentData.builder()
                .id(10L)
                .provider("ghn")
                .trackingCode("GHN123")
                .status(ShipmentStatus.READY_TO_PICK)
                .build();

        when(shipmentRepository.findByProviderAndTrackingCode("ghn", "GHN123"))
                .thenReturn(Optional.of(shipment));
        when(shipmentRepository.save(any(ShipmentData.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        useCase.execute("ghn", Map.of(
                "order_code", "GHN123",
                "status", "delivering"
        ));

        assertThat(shipment.getStatus()).isEqualTo(ShipmentStatus.DELIVERING);
        assertThat(shipment.getShippedAt()).isNotNull();
    }

    @Test
    void keepsDocumentedGhnStatusInsteadOfCollapsingIt() {
        ShipmentData shipment = ShipmentData.builder()
                .id(10L)
                .provider("ghn")
                .trackingCode("GHN123")
                .status(ShipmentStatus.READY_TO_PICK)
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

        assertThat(shipment.getStatus()).isEqualTo(ShipmentStatus.RETURN_TRANSPORTING);
        assertThat(shipment.getShippedAt()).isNotNull();
        verify(shipmentRepository).addLog(10L, ShipmentStatus.RETURN_TRANSPORTING, "return_transporting", ShipmentLogSource.WEBHOOK, "Buu cuc GHN",
                "Webhook GHN: return_transporting");
    }

    @Test
    void delegatesGhtkPayloadToDedicatedProcessor() {
        Map<String, Object> payload = Map.of("label_id", "S1.A1.17373471", "status_id", 5);
        when(ghtkWebhookProcessor.process(payload)).thenReturn(Map.of("ok", true, "status", "delivered"));

        Map<String, Object> result = useCase.execute("GHTK", payload);

        assertThat(result).containsEntry("status", "delivered");
        verify(ghtkWebhookProcessor).process(payload);
        verifyNoInteractions(shipmentRepository, auditLogService, shipmentOrderStatusSyncService);
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
