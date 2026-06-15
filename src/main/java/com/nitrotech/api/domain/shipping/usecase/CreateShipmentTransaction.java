package com.nitrotech.api.domain.shipping.usecase;

import com.nitrotech.api.domain.audit.dto.AuditAction;
import com.nitrotech.api.domain.audit.dto.AuditLogCommand;
import com.nitrotech.api.domain.audit.dto.AuditResourceType;
import com.nitrotech.api.domain.audit.service.AuditLogService;
import com.nitrotech.api.domain.shipping.dto.ShipmentData;
import com.nitrotech.api.domain.shipping.dto.ShipmentLogSource;
import com.nitrotech.api.domain.shipping.dto.ShipmentStatus;
import com.nitrotech.api.domain.shipping.repository.ShipmentRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Map;

@Service
@RequiredArgsConstructor
public class CreateShipmentTransaction {

    private final ShipmentRepository shipmentRepository;
    private final AuditLogService auditLogService;

    @Transactional
    public ShipmentData save(ShipmentData shipment, String providerName, Long orderId) {
        ShipmentData savedShipment = shipmentRepository.save(shipment);

        shipmentRepository.addLog(
                savedShipment.getId(),
                ShipmentStatus.READY_TO_PICK,
                ShipmentStatus.READY_TO_PICK.value(),
                ShipmentLogSource.ADMIN,
                null,
                "Vận đơn được khởi tạo thành công qua đối tác " + providerName.toUpperCase()
        );
        auditLogService.record(AuditLogCommand.success(
                AuditAction.SHIPMENT_CREATED,
                AuditResourceType.SHIPMENT,
                savedShipment.getId(),
                null,
                Map.of(
                        "orderId", savedShipment.getOrderId(),
                        "provider", savedShipment.getProvider(),
                        "trackingCode", savedShipment.getTrackingCode(),
                        "status", savedShipment.getStatus().value()
                ),
                Map.of("orderId", orderId)
        ));

        return savedShipment;
    }
}
