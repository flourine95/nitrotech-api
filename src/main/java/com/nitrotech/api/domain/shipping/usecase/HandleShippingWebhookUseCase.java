package com.nitrotech.api.domain.shipping.usecase;

import com.nitrotech.api.domain.audit.dto.AuditLogCommand;
import com.nitrotech.api.domain.audit.dto.AuditAction;
import com.nitrotech.api.domain.audit.dto.AuditActorType;
import com.nitrotech.api.domain.audit.dto.AuditOutcome;
import com.nitrotech.api.domain.audit.dto.AuditResourceType;
import com.nitrotech.api.domain.audit.service.AuditLogService;
import com.nitrotech.api.domain.order.dto.OrderData;
import com.nitrotech.api.domain.order.repository.OrderRepository;
import com.nitrotech.api.domain.shipping.dto.ShipmentData;
import com.nitrotech.api.domain.shipping.dto.ShipmentLogSource;
import com.nitrotech.api.domain.shipping.dto.ShipmentStatus;
import com.nitrotech.api.domain.shipping.repository.ShipmentRepository;
import com.nitrotech.api.shared.exception.BadRequestException;
import com.nitrotech.api.shared.exception.NotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.Locale;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class HandleShippingWebhookUseCase {

    private final ShipmentRepository shipmentRepository;
    private final OrderRepository orderRepository;
    private final AuditLogService auditLogService;

    @Transactional
    public Map<String, Object> execute(String provider, Map<String, Object> payload) {
        if (provider == null || provider.isBlank()) {
            throw new BadRequestException("INVALID_SHIPPING_PROVIDER", "Shipping provider is required");
        }

        String normalizedProvider = provider.trim().toLowerCase(Locale.ROOT);
        String trackingCode = firstString(payload, "OrderCode", "order_code", "orderCode",
                "tracking_code", "trackingCode", "label_id");
        String providerStatus = firstString(payload, "Status", "status", "CurrentStatus", "current_status", "status_id");
        String location = firstString(payload, "Warehouse", "warehouse", "Location", "location");
        String type = firstString(payload, "Type", "type");
        if (type == null && "ghtk".equals(normalizedProvider)) {
            type = "status_" + providerStatus;
        }

        if (trackingCode == null || providerStatus == null) {
            throw new BadRequestException("INVALID_SHIPPING_WEBHOOK",
                    "Shipping webhook must include tracking code and status");
        }

        ShipmentData shipment = shipmentRepository.findByProviderAndTrackingCode(normalizedProvider, trackingCode)
                .orElseThrow(() -> new NotFoundException("SHIPMENT_NOT_FOUND",
                        "Shipment with tracking code " + trackingCode + " not found"));

        ShipmentStatus status = mapStatus(normalizedProvider, providerStatus);
        ShipmentStatus previousStatus = shipment.getStatus();
        shipment.setStatus(status);
        if (isInTransit(status) && shipment.getShippedAt() == null) {
            shipment.setShippedAt(Instant.now());
        }
        if (ShipmentStatus.DELIVERED == status && shipment.getDeliveredAt() == null) {
            shipment.setDeliveredAt(Instant.now());
        }

        ShipmentData saved = shipmentRepository.save(shipment);
        String note = "Webhook " + normalizedProvider.toUpperCase(Locale.ROOT) + ": " + providerStatus;
        if (type != null) {
            note += " (" + type + ")";
        }
        String reason = firstString(payload, "reason", "Reason");
        if (reason != null) {
            note += " - " + reason;
        }
        shipmentRepository.addLog(saved.getId(), status, providerStatus, ShipmentLogSource.WEBHOOK, location, note);
        syncOrderStatus(saved, status);
        auditLogService.record(new AuditLogCommand(
                AuditActorType.WEBHOOK,
                null,
                null,
                AuditAction.SHIPMENT_WEBHOOK_RECEIVED,
                AuditResourceType.SHIPMENT,
                String.valueOf(saved.getId()),
                AuditOutcome.SUCCESS,
                Map.of("status", previousStatus.value()),
                Map.of("status", saved.getStatus().value(), "rawStatus", providerStatus),
                Map.of(
                        "provider", normalizedProvider,
                        "trackingCode", trackingCode,
                        "type", type == null ? "" : type
                )
        ));

        return Map.of(
                "ok", true,
                "shipmentId", saved.getId(),
                "trackingCode", saved.getTrackingCode(),
                "status", saved.getStatus().value()
        );
    }

    private String firstString(Map<String, Object> payload, String... keys) {
        for (String key : keys) {
            Object value = payload.get(key);
            if (value instanceof String text && !text.isBlank()) {
                return text.trim();
            }
            if (value instanceof Number number) {
                return String.valueOf(number);
            }
        }
        return null;
    }

    private ShipmentStatus mapStatus(String provider, String providerStatus) {
        if ("ghtk".equals(provider)) {
            return mapGhtkStatus(providerStatus);
        }

        String normalized = providerStatus.trim().toLowerCase(Locale.ROOT)
                .replace('-', '_')
                .replace(' ', '_');

        return switch (normalized) {
            case "cancelled", "canceled" -> ShipmentStatus.CANCEL;
            case "delivery_success", "delivered_success" -> ShipmentStatus.DELIVERED;
            default -> ShipmentStatus.fromValue(normalized);
        };
    }

    private ShipmentStatus mapGhtkStatus(String statusId) {
        String normalized = statusId.trim();
        return switch (normalized) {
            case "-1" -> ShipmentStatus.CANCEL;
            case "1", "2" -> ShipmentStatus.READY_TO_PICK;
            case "3", "12", "123" -> ShipmentStatus.PICKED;
            case "4", "45" -> ShipmentStatus.DELIVERING;
            case "5", "6" -> ShipmentStatus.DELIVERED;
            case "7", "8", "127", "128" -> ShipmentStatus.PICKUP_FAILED;
            case "9", "10", "49", "410" -> ShipmentStatus.DELIVERY_FAILED;
            case "11", "20" -> ShipmentStatus.RETURNING;
            case "21" -> ShipmentStatus.RETURNED;
            case "13" -> ShipmentStatus.COMPENSATING;
            default -> ShipmentStatus.UNKNOWN;
        };
    }

    private boolean isInTransit(ShipmentStatus status) {
        return switch (status) {
            case PICKED, STORING, TRANSPORTING, SORTING, DELIVERING,
                 MONEY_COLLECT_DELIVERING, WAITING_TO_RETURN, RETURN,
                 RETURN_TRANSPORTING, RETURN_SORTING, RETURNING -> true;
            default -> false;
        };
    }

    private void syncOrderStatus(ShipmentData shipment, ShipmentStatus shipmentStatus) {
        orderRepository.findById(shipment.getOrderId()).ifPresent(order -> {
            if ("cancelled".equals(order.status())) {
                return;
            }
            if (ShipmentStatus.DELIVERED == shipmentStatus && !"delivered".equals(order.status())) {
                orderRepository.updateStatus(order.id(), "delivered");
                return;
            }
            if (isInTransit(shipmentStatus) && shouldMarkProcessing(order)) {
                orderRepository.updateStatus(order.id(), "processing");
            }
        });
    }

    private boolean shouldMarkProcessing(OrderData order) {
        return "confirmed".equals(order.status()) || "pending".equals(order.status());
    }
}
