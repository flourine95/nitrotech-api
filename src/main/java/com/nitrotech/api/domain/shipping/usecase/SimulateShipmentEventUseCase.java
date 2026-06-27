package com.nitrotech.api.domain.shipping.usecase;

import com.nitrotech.api.domain.shipping.exception.ShipmentNotFoundException;

import com.nitrotech.api.domain.audit.AuditAction;
import com.nitrotech.api.domain.audit.dto.AuditLogCommand;
import com.nitrotech.api.domain.audit.AuditResourceType;
import com.nitrotech.api.domain.audit.service.AuditLogService;
import com.nitrotech.api.domain.shipping.dto.ShipmentData;
import com.nitrotech.api.domain.shipping.dto.ShipmentLogSource;
import com.nitrotech.api.domain.shipping.ShipmentStatus;
import com.nitrotech.api.domain.shipping.repository.ShipmentRepository;
import com.nitrotech.api.domain.shipping.service.ShipmentOrderStatusSyncService;
import com.nitrotech.api.shared.exception.BadRequestException;
import com.nitrotech.api.shared.exception.ForbiddenException;
import com.nitrotech.api.shared.exception.NotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.Map;
import java.util.Set;

@Service
@RequiredArgsConstructor
public class SimulateShipmentEventUseCase {

    private static final Map<ShipmentStatus, Set<ShipmentStatus>> ALLOWED_TRANSITIONS = Map.ofEntries(
            Map.entry(ShipmentStatus.UNKNOWN, Set.of(ShipmentStatus.READY_TO_PICK)),
            Map.entry(ShipmentStatus.READY_TO_PICK, Set.of(ShipmentStatus.PICKED, ShipmentStatus.CANCEL)),
            Map.entry(ShipmentStatus.PICKED, Set.of(
                    ShipmentStatus.STORING,
                    ShipmentStatus.TRANSPORTING,
                    ShipmentStatus.DELIVERING,
                    ShipmentStatus.DELIVERY_FAILED,
                    ShipmentStatus.CANCEL
            )),
            Map.entry(ShipmentStatus.STORING, Set.of(
                    ShipmentStatus.TRANSPORTING,
                    ShipmentStatus.SORTING,
                    ShipmentStatus.DELIVERING,
                    ShipmentStatus.DELIVERY_FAILED
            )),
            Map.entry(ShipmentStatus.TRANSPORTING, Set.of(
                    ShipmentStatus.SORTING,
                    ShipmentStatus.DELIVERING,
                    ShipmentStatus.DELIVERY_FAILED
            )),
            Map.entry(ShipmentStatus.SORTING, Set.of(
                    ShipmentStatus.DELIVERING,
                    ShipmentStatus.DELIVERY_FAILED
            )),
            Map.entry(ShipmentStatus.DELIVERING, Set.of(
                    ShipmentStatus.DELIVERED,
                    ShipmentStatus.DELIVERY_FAILED,
                    ShipmentStatus.WAITING_TO_RETURN
            )),
            Map.entry(ShipmentStatus.MONEY_COLLECT_DELIVERING, Set.of(
                    ShipmentStatus.DELIVERED,
                    ShipmentStatus.DELIVERY_FAILED,
                    ShipmentStatus.WAITING_TO_RETURN
            )),
            Map.entry(ShipmentStatus.DELIVERY_FAILED, Set.of(
                    ShipmentStatus.DELIVERING,
                    ShipmentStatus.WAITING_TO_RETURN,
                    ShipmentStatus.RETURNING
            )),
            Map.entry(ShipmentStatus.WAITING_TO_RETURN, Set.of(ShipmentStatus.RETURNING)),
            Map.entry(ShipmentStatus.RETURN, Set.of(ShipmentStatus.RETURN_TRANSPORTING)),
            Map.entry(ShipmentStatus.RETURNING, Set.of(ShipmentStatus.RETURN_TRANSPORTING, ShipmentStatus.RETURNED)),
            Map.entry(ShipmentStatus.RETURN_TRANSPORTING, Set.of(ShipmentStatus.RETURN_SORTING, ShipmentStatus.RETURNED)),
            Map.entry(ShipmentStatus.RETURN_SORTING, Set.of(ShipmentStatus.RETURNED))
    );

    private final ShipmentRepository shipmentRepository;
    private final ShipmentOrderStatusSyncService shipmentOrderStatusSyncService;
    private final AuditLogService auditLogService;

    @Value("${app.shipping.simulation-enabled:false}")
    private boolean simulationEnabled;

    @Transactional
    public ShipmentData execute(Long shipmentId, String requestedStatus, String location, String note) {
        if (!simulationEnabled) {
            throw new ForbiddenException("SHIPMENT_SIMULATION_DISABLED", "Shipment simulation is disabled");
        }

        ShipmentStatus nextStatus = ShipmentStatus.fromValue(requestedStatus);
        if (ShipmentStatus.UNKNOWN == nextStatus && !"unknown".equalsIgnoreCase(requestedStatus.trim())) {
            throw new BadRequestException("SHIPMENT_STATUS_INVALID", "Shipment status is invalid");
        }

        ShipmentData shipment = shipmentRepository.findById(shipmentId)
                .orElseThrow(() -> new ShipmentNotFoundException());
        ShipmentStatus currentStatus = shipment.getStatus();

        if (currentStatus == nextStatus) {
            return shipment;
        }
        if (!isTransitionAllowed(currentStatus, nextStatus)) {
            throw new BadRequestException(
                    "SHIPMENT_STATUS_TRANSITION_INVALID",
                    "Cannot transition shipment from " + currentStatus.value() + " to " + nextStatus.value()
            );
        }

        shipment.setStatus(nextStatus);
        if (isInTransit(nextStatus) && shipment.getShippedAt() == null) {
            shipment.setShippedAt(Instant.now());
        }
        if (ShipmentStatus.DELIVERED == nextStatus && shipment.getDeliveredAt() == null) {
            shipment.setDeliveredAt(Instant.now());
        }

        ShipmentData saved = shipmentRepository.save(shipment);
        shipmentRepository.addLog(
                saved.getId(),
                nextStatus,
                nextStatus.value(),
                ShipmentLogSource.SIMULATION,
                location,
                simulationNote(note)
        );
        if (ShipmentStatus.DELIVERED == nextStatus) {
            shipmentOrderStatusSyncService.syncDeliveredShipment(saved, "Shipment simulation");
        }

        auditLogService.record(AuditLogCommand.success(
                AuditAction.SHIPMENT_SIMULATION_EVENT,
                AuditResourceType.SHIPMENT,
                saved.getId(),
                Map.of(
                        "status", currentStatus.value(),
                        "simulated", true
                ),
                Map.of(
                        "status", saved.getStatus().value(),
                        "simulated", true
                ),
                Map.of(
                        "orderId", saved.getOrderId(),
                        "location", location == null ? "" : location,
                        "note", note == null ? "" : note,
                        "simulated", true
                )
        ));

        return saved;
    }

    private boolean isTransitionAllowed(ShipmentStatus currentStatus, ShipmentStatus nextStatus) {
        return ALLOWED_TRANSITIONS.getOrDefault(currentStatus, Set.of()).contains(nextStatus);
    }

    private boolean isInTransit(ShipmentStatus status) {
        return switch (status) {
            case PICKED, STORING, TRANSPORTING, SORTING, DELIVERING,
                 MONEY_COLLECT_DELIVERING, WAITING_TO_RETURN, RETURN,
                 RETURN_TRANSPORTING, RETURN_SORTING, RETURNING -> true;
            default -> false;
        };
    }

    private String simulationNote(String note) {
        if (note == null || note.isBlank()) {
            return "Simulated shipment event";
        }
        return "Simulated shipment event - " + note.trim();
    }
}
