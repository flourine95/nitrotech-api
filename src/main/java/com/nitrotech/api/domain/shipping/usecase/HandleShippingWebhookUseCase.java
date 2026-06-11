package com.nitrotech.api.domain.shipping.usecase;

import com.nitrotech.api.domain.shipping.dto.ShipmentData;
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

    @Transactional
    public Map<String, Object> execute(String provider, Map<String, Object> payload) {
        if (provider == null || provider.isBlank()) {
            throw new BadRequestException("INVALID_SHIPPING_PROVIDER", "Shipping provider is required");
        }

        String trackingCode = firstString(payload, "OrderCode", "order_code", "orderCode", "tracking_code", "trackingCode");
        String providerStatus = firstString(payload, "Status", "status", "CurrentStatus", "current_status");
        String location = firstString(payload, "Warehouse", "warehouse", "Location", "location");
        String type = firstString(payload, "Type", "type");

        if (trackingCode == null || providerStatus == null) {
            throw new BadRequestException("INVALID_SHIPPING_WEBHOOK",
                    "Shipping webhook must include tracking code and status");
        }

        ShipmentData shipment = shipmentRepository.findByProviderAndTrackingCode(provider, trackingCode)
                .orElseThrow(() -> new NotFoundException("SHIPMENT_NOT_FOUND",
                        "Shipment with tracking code " + trackingCode + " not found"));

        String status = mapStatus(providerStatus);
        shipment.setStatus(status);
        if (isInTransit(status) && shipment.getShippedAt() == null) {
            shipment.setShippedAt(Instant.now());
        }
        if ("delivered".equals(status) && shipment.getDeliveredAt() == null) {
            shipment.setDeliveredAt(Instant.now());
        }

        ShipmentData saved = shipmentRepository.save(shipment);
        String note = "Webhook " + provider.toUpperCase(Locale.ROOT) + ": " + providerStatus;
        if (type != null) {
            note += " (" + type + ")";
        }
        shipmentRepository.addLog(saved.getId(), status, location, note);

        return Map.of(
                "ok", true,
                "shipmentId", saved.getId(),
                "trackingCode", saved.getTrackingCode(),
                "status", saved.getStatus()
        );
    }

    private String firstString(Map<String, Object> payload, String... keys) {
        for (String key : keys) {
            Object value = payload.get(key);
            if (value instanceof String text && !text.isBlank()) {
                return text.trim();
            }
        }
        return null;
    }

    private String mapStatus(String providerStatus) {
        String normalized = providerStatus.trim().toLowerCase(Locale.ROOT)
                .replace('-', '_')
                .replace(' ', '_');

        return switch (normalized) {
            case "cancelled", "canceled" -> "cancel";
            case "delivery_success", "delivered_success" -> "delivered";
            default -> normalized;
        };
    }

    private boolean isInTransit(String status) {
        return switch (status) {
            case "picked", "storing", "transporting", "sorting", "delivering",
                 "money_collect_delivering", "waiting_to_return", "return",
                 "return_transporting", "return_sorting", "returning" -> true;
            default -> false;
        };
    }
}
