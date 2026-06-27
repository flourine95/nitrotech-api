package com.nitrotech.api.domain.shipping.usecase;

import com.nitrotech.api.domain.audit.dto.AuditLogCommand;
import com.nitrotech.api.domain.audit.AuditAction;
import com.nitrotech.api.domain.audit.AuditActorType;
import com.nitrotech.api.domain.audit.AuditOutcome;
import com.nitrotech.api.domain.audit.AuditResourceType;
import com.nitrotech.api.domain.audit.service.AuditLogService;
import com.nitrotech.api.domain.shipping.dto.ShipmentData;
import com.nitrotech.api.domain.shipping.dto.ShipmentLogSource;
import com.nitrotech.api.domain.shipping.ShipmentStatus;
import com.nitrotech.api.domain.shipping.exception.ShipmentNotFoundException;
import com.nitrotech.api.domain.shipping.repository.ShipmentRepository;
import com.nitrotech.api.domain.shipping.service.ShipmentOrderStatusSyncService;
import com.nitrotech.api.shared.exception.BadRequestException;
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
    private final AuditLogService auditLogService;
    private final ShipmentOrderStatusSyncService shipmentOrderStatusSyncService;
    private final GhtkWebhookProcessor ghtkWebhookProcessor;

    @Transactional
    public Map<String, Object> execute(String provider, Map<String, Object> payload) {
        if (provider == null || provider.isBlank()) {
            throw new BadRequestException("INVALID_SHIPPING_PROVIDER", "Shipping provider is required");
        }

        String normalizedProvider = provider.trim().toLowerCase(Locale.ROOT);
        if ("ghtk".equals(normalizedProvider)) {
            return ghtkWebhookProcessor.process(payload);
        }
        String trackingCode = firstString(payload, "OrderCode", "order_code", "orderCode",
                "tracking_code", "trackingCode", "label_id");
        String providerStatus = firstString(payload, "Status", "status", "CurrentStatus", "current_status", "status_id");
        String location = firstString(payload, "Warehouse", "warehouse", "Location", "location");
        String type = firstString(payload, "Type", "type");

        if (trackingCode == null || providerStatus == null) {
            throw new BadRequestException("INVALID_SHIPPING_WEBHOOK",
                    "Shipping webhook must include tracking code and status");
        }

        ShipmentData shipment = shipmentRepository.findByProviderAndTrackingCode(normalizedProvider, trackingCode)
                .orElseThrow(() -> ShipmentNotFoundException.withTrackingCode(trackingCode));

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
        if (ShipmentStatus.DELIVERED == status) {
            shipmentOrderStatusSyncService.syncDeliveredShipment(
                    saved,
                    "Shipping webhook " + normalizedProvider.toUpperCase(Locale.ROOT)
            );
        }
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
        String normalized = providerStatus.trim().toLowerCase(Locale.ROOT)
                .replace('-', '_')
                .replace(' ', '_');

        return switch (normalized) {
            case "cancelled", "canceled" -> ShipmentStatus.CANCEL;
            case "delivery_success", "delivered_success" -> ShipmentStatus.DELIVERED;
            default -> ShipmentStatus.fromValue(normalized);
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

}
