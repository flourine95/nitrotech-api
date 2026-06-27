package com.nitrotech.api.domain.shipping.usecase;

import com.nitrotech.api.domain.audit.dto.AuditLogCommand;
import com.nitrotech.api.domain.audit.service.AuditLogService;
import com.nitrotech.api.domain.shipping.dto.ShipmentData;
import com.nitrotech.api.domain.shipping.ShipmentStatus;
import com.nitrotech.api.domain.shipping.repository.ShipmentRepository;
import com.nitrotech.api.domain.shipping.service.ShipmentOrderStatusSyncService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.Map;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

class GhtkWebhookProcessorTest {

    private ShipmentRepository shipmentRepository;
    private ShipmentOrderStatusSyncService orderStatusSyncService;
    private AuditLogService auditLogService;
    private GhtkWebhookProcessor processor;

    @BeforeEach
    void setUp() {
        shipmentRepository = mock(ShipmentRepository.class);
        orderStatusSyncService = mock(ShipmentOrderStatusSyncService.class);
        auditLogService = mock(AuditLogService.class);
        processor = new GhtkWebhookProcessor(
                shipmentRepository,
                orderStatusSyncService,
                auditLogService
        );
    }

    @Test
    void notificationOnlyStatusCreatesTimelineEventWithoutChangingShipment() {
        ShipmentData shipment = shipment(ShipmentStatus.DELIVERING);
        when(shipmentRepository.findByProviderAndTrackingCode("ghtk", "S1.A1.17373471"))
                .thenReturn(Optional.of(shipment));
        when(shipmentRepository.recordWebhookEvent(anyLong(), any(), anyString(), any(), any(), any(), any(), any()))
                .thenReturn(true);

        Map<String, Object> result = processor.process(payload(10));

        assertThat(result).containsEntry("status", "delivering");
        verify(shipmentRepository, never()).save(any());
        verifyNoInteractions(orderStatusSyncService);
        verify(auditLogService).record(any(AuditLogCommand.class));
    }

    @Test
    void duplicateEventReturnsSuccessWithoutRepeatingSideEffects() {
        ShipmentData shipment = shipment(ShipmentStatus.DELIVERING);
        when(shipmentRepository.findByProviderAndTrackingCode("ghtk", "S1.A1.17373471"))
                .thenReturn(Optional.of(shipment));
        when(shipmentRepository.recordWebhookEvent(anyLong(), any(), anyString(), any(), any(), any(), any(), any()))
                .thenReturn(false);

        Map<String, Object> result = processor.process(payload(5));

        assertThat(result).containsEntry("duplicate", true);
        verify(shipmentRepository, never()).save(any());
        verifyNoInteractions(orderStatusSyncService, auditLogService);
    }

    @Test
    void oldOfficialEventIsLoggedButCannotOverwriteNewerStatus() {
        ShipmentData shipment = shipment(ShipmentStatus.RETURNED);
        shipment.setLastOfficialEventAt(Instant.parse("2026-06-20T10:00:00Z"));
        when(shipmentRepository.findByProviderAndTrackingCode("ghtk", "S1.A1.17373471"))
                .thenReturn(Optional.of(shipment));
        when(shipmentRepository.recordWebhookEvent(anyLong(), any(), anyString(), any(), any(), any(), any(), any()))
                .thenReturn(true);

        processor.process(payload(5));

        assertThat(shipment.getStatus()).isEqualTo(ShipmentStatus.RETURNED);
        verify(shipmentRepository, never()).save(any());
        verifyNoInteractions(orderStatusSyncService);
    }

    @Test
    void partialDeliveryDoesNotCompleteSalesOrder() {
        ShipmentData shipment = shipment(ShipmentStatus.DELIVERING);
        when(shipmentRepository.findByProviderAndTrackingCode("ghtk", "S1.A1.17373471"))
                .thenReturn(Optional.of(shipment));
        when(shipmentRepository.recordWebhookEvent(anyLong(), any(), anyString(), any(), any(), any(), any(), any()))
                .thenReturn(true);
        when(shipmentRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));

        processor.process(Map.of(
                "label_id", "S1.A1.17373471",
                "status_id", 5,
                "action_time", "2026-06-20T16:00:00+07:00",
                "return_part_package", 1
        ));

        assertThat(shipment.getStatus()).isEqualTo(ShipmentStatus.DELIVERED);
        verifyNoInteractions(orderStatusSyncService);
    }

    @Test
    void officialEventWithoutActionTimeIsLoggedButCannotChangeCurrentStatus() {
        ShipmentData shipment = shipment(ShipmentStatus.DELIVERING);
        when(shipmentRepository.findByProviderAndTrackingCode("ghtk", "S1.A1.17373471"))
                .thenReturn(Optional.of(shipment));
        when(shipmentRepository.recordWebhookEvent(anyLong(), any(), anyString(), any(), any(), any(), any(), any()))
                .thenReturn(true);

        processor.process(Map.of("label_id", "S1.A1.17373471", "status_id", 5));

        assertThat(shipment.getStatus()).isEqualTo(ShipmentStatus.DELIVERING);
        verify(shipmentRepository, never()).save(any());
        verifyNoInteractions(orderStatusSyncService);
    }

    private ShipmentData shipment(ShipmentStatus status) {
        return ShipmentData.builder()
                .id(20L)
                .orderId(123L)
                .provider("ghtk")
                .trackingCode("S1.A1.17373471")
                .status(status)
                .build();
    }

    private Map<String, Object> payload(int statusId) {
        return Map.of(
                "label_id", "S1.A1.17373471",
                "status_id", statusId,
                "action_time", "2026-06-20T16:00:00+07:00"
        );
    }
}
