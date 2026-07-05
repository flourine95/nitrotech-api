package com.nitrotech.api.domain.shipping.usecase;

import com.nitrotech.api.domain.audit.dto.AuditLogCommand;
import com.nitrotech.api.domain.audit.service.AuditLogService;
import com.nitrotech.api.domain.shipping.dto.ShipmentData;
import com.nitrotech.api.domain.shipping.dto.ShipmentLogSource;
import com.nitrotech.api.domain.shipping.ShipmentStatus;
import com.nitrotech.api.domain.shipping.repository.ShipmentRepository;
import com.nitrotech.api.domain.shipping.service.ShipmentOrderStatusSyncService;
import com.nitrotech.api.shared.exception.BadRequestException;
import com.nitrotech.api.shared.exception.ForbiddenException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import java.math.BigDecimal;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.*;

class SimulateShipmentEventUseCaseTest {

    private ShipmentRepository shipmentRepository;
    private ShipmentOrderStatusSyncService shipmentOrderStatusSyncService;
    private AuditLogService auditLogService;
    private SimulateShipmentEventUseCase useCase;

    @BeforeEach
    void setUp() {
        shipmentRepository = mock(ShipmentRepository.class);
        shipmentOrderStatusSyncService = mock(ShipmentOrderStatusSyncService.class);
        auditLogService = mock(AuditLogService.class);
        useCase = new SimulateShipmentEventUseCase(
                shipmentRepository,
                shipmentOrderStatusSyncService,
                auditLogService
        );
    }

    @Test
    void rejectsWhenSimulationIsDisabled() {
        ReflectionTestUtils.setField(useCase, "simulationEnabled", false);

        assertThatThrownBy(() -> useCase.execute(10L, "delivering", "Demo", "Demo event"))
                .isInstanceOf(ForbiddenException.class)
                .hasMessage("Shipment simulation is disabled");

        verifyNoInteractions(shipmentRepository, shipmentOrderStatusSyncService, auditLogService);
    }

    @Test
    void rejectsInvalidTransition() {
        ReflectionTestUtils.setField(useCase, "simulationEnabled", true);
        ShipmentData shipment = shipment(ShipmentStatus.DELIVERED);
        when(shipmentRepository.findById(10L)).thenReturn(Optional.of(shipment));

        assertThatThrownBy(() -> useCase.execute(10L, "delivering", "Demo", "Demo event"))
                .isInstanceOf(BadRequestException.class)
                .hasMessage("Cannot transition shipment from delivered to delivering");

        verify(shipmentRepository, never()).save(any());
        verifyNoInteractions(shipmentOrderStatusSyncService, auditLogService);
    }

    @Test
    void returnsCurrentShipmentWithoutDuplicateLogWhenStatusIsUnchanged() {
        ReflectionTestUtils.setField(useCase, "simulationEnabled", true);
        ShipmentData shipment = shipment(ShipmentStatus.DELIVERING);
        when(shipmentRepository.findById(10L)).thenReturn(Optional.of(shipment));

        ShipmentData result = useCase.execute(10L, "delivering", "Demo", "Duplicate click");

        assertThat(result).isSameAs(shipment);
        verify(shipmentRepository, never()).save(any());
        verify(shipmentRepository, never()).addLog(anyLong(), any(), anyString(), any(), any(), any());
        verifyNoInteractions(shipmentOrderStatusSyncService, auditLogService);
    }

    @Test
    void simulatesEventWithoutSyncingOrderBeforeDelivery() {
        ReflectionTestUtils.setField(useCase, "simulationEnabled", true);
        ShipmentData shipment = shipment(ShipmentStatus.PICKED);
        when(shipmentRepository.findById(10L)).thenReturn(Optional.of(shipment));
        when(shipmentRepository.save(any(ShipmentData.class))).thenAnswer(invocation -> invocation.getArgument(0));

        ShipmentData result = useCase.execute(10L, "delivering", "Demo Hub", "Demo order is out for delivery");

        assertThat(result.getStatus()).isEqualTo(ShipmentStatus.DELIVERING);
        assertThat(result.getShippedAt()).isNotNull();
        verify(shipmentRepository).addLog(
                10L,
                ShipmentStatus.DELIVERING,
                "delivering",
                ShipmentLogSource.SIMULATION,
                "Demo Hub",
                "Demo order is out for delivery"
        );
        verifyNoInteractions(shipmentOrderStatusSyncService);
        verify(auditLogService).record(any(AuditLogCommand.class));
    }

    private ShipmentData shipment(ShipmentStatus status) {
        return ShipmentData.builder()
                .id(10L)
                .orderId(123L)
                .provider("ghtk")
                .trackingCode("S1.123")
                .status(status)
                .fee(BigDecimal.ZERO)
                .build();
    }
}
