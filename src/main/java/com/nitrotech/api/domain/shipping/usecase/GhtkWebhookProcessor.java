package com.nitrotech.api.domain.shipping.usecase;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.nitrotech.api.domain.audit.dto.AuditAction;
import com.nitrotech.api.domain.audit.dto.AuditLogCommand;
import com.nitrotech.api.domain.audit.dto.AuditResourceType;
import com.nitrotech.api.domain.audit.service.AuditLogService;
import com.nitrotech.api.domain.shipping.dto.ShipmentData;
import com.nitrotech.api.domain.shipping.dto.ShipmentStatus;
import com.nitrotech.api.domain.shipping.repository.ShipmentRepository;
import com.nitrotech.api.domain.shipping.service.ShipmentOrderStatusSyncService;
import com.nitrotech.api.shared.exception.BadRequestException;
import com.nitrotech.api.shared.exception.NotFoundException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.util.Map;
import java.util.Set;

@Slf4j
@Service
@RequiredArgsConstructor
public class GhtkWebhookProcessor {

    private static final Map<ShipmentStatus, Set<ShipmentStatus>> ALLOWED_TRANSITIONS = Map.ofEntries(
            Map.entry(ShipmentStatus.UNKNOWN, Set.of(ShipmentStatus.READY_TO_PICK, ShipmentStatus.CANCEL)),
            Map.entry(ShipmentStatus.READY_TO_PICK, Set.of(
                    ShipmentStatus.PICKED, ShipmentStatus.DELIVERING,
                    ShipmentStatus.PICKUP_FAILED, ShipmentStatus.CANCEL
            )),
            Map.entry(ShipmentStatus.PICKED, Set.of(
                    ShipmentStatus.DELIVERING, ShipmentStatus.PICKUP_FAILED,
                    ShipmentStatus.CANCEL
            )),
            Map.entry(ShipmentStatus.DELIVERING, Set.of(
                    ShipmentStatus.DELIVERED, ShipmentStatus.DELIVERY_FAILED,
                    ShipmentStatus.RETURNING
            )),
            Map.entry(ShipmentStatus.DELIVERY_FAILED, Set.of(
                    ShipmentStatus.DELIVERING, ShipmentStatus.RETURNING,
                    ShipmentStatus.RETURNED
            )),
            Map.entry(ShipmentStatus.RETURNING, Set.of(ShipmentStatus.RETURNED)),
            Map.entry(ShipmentStatus.PICKUP_FAILED, Set.of(ShipmentStatus.CANCEL)),
            Map.entry(ShipmentStatus.COMPENSATING, Set.of()),
            Map.entry(ShipmentStatus.CANCEL, Set.of()),
            Map.entry(ShipmentStatus.DELIVERED, Set.of()),
            Map.entry(ShipmentStatus.RETURNED, Set.of())
    );

    private final ShipmentRepository shipmentRepository;
    private final ShipmentOrderStatusSyncService shipmentOrderStatusSyncService;
    private final AuditLogService auditLogService;
    private final ObjectMapper objectMapper = new ObjectMapper();

    @Transactional
    public Map<String, Object> process(Map<String, Object> payload) {
        String trackingCode = required(payload, "label_id");
        String rawStatus = required(payload, "status_id");
        String reasonCode = firstString(payload, "reason_code");
        String reason = firstString(payload, "reason");
        Instant actionTime = parseActionTime(firstString(payload, "action_time"));
        int statusId = parseStatusId(rawStatus);

        ShipmentData shipment = shipmentRepository.findByProviderAndTrackingCode("ghtk", trackingCode)
                .orElseThrow(() -> new NotFoundException("SHIPMENT_NOT_FOUND",
                        "Shipment with tracking code " + trackingCode + " not found"));

        ShipmentStatus officialStatus = officialStatus(statusId);
        ShipmentStatus logStatus = officialStatus == null ? shipment.getStatus() : officialStatus;
        boolean inserted = shipmentRepository.recordWebhookEvent(
                shipment.getId(),
                logStatus,
                rawStatus,
                actionTime,
                reasonCode,
                buildNote(statusId, reason),
                serialize(payload),
                eventKey(trackingCode, rawStatus, actionTime)
        );
        if (!inserted) {
            return Map.of(
                    "ok", true,
                    "duplicate", true,
                    "shipmentId", shipment.getId(),
                    "trackingCode", shipment.getTrackingCode(),
                    "status", shipment.getStatus().value()
            );
        }

        ShipmentStatus previousStatus = shipment.getStatus();
        ShipmentData saved = shipment;
        if (officialStatus != null && shouldApplyOfficialStatus(shipment, officialStatus, actionTime)) {
            shipment.setStatus(officialStatus);
            shipment.setLastOfficialEventAt(actionTime);
            if (officialStatus == ShipmentStatus.DELIVERING && shipment.getShippedAt() == null) {
                shipment.setShippedAt(Instant.now());
            }
            if (officialStatus == ShipmentStatus.DELIVERED && shipment.getDeliveredAt() == null) {
                shipment.setDeliveredAt(Instant.now());
            }
            saved = shipmentRepository.save(shipment);

            if (officialStatus == ShipmentStatus.DELIVERED && !isPartialDelivery(payload)) {
                shipmentOrderStatusSyncService.syncDeliveredShipment(saved, "GHTK status_id=5");
            }
        }

        auditLogService.record(AuditLogCommand.success(
                AuditAction.SHIPMENT_WEBHOOK_RECEIVED,
                AuditResourceType.SHIPMENT,
                shipment.getId(),
                Map.of("status", previousStatus.value()),
                Map.of("status", saved.getStatus().value(), "rawStatus", rawStatus),
                Map.of(
                        "provider", "ghtk",
                        "trackingCode", trackingCode,
                        "statusId", statusId,
                        "actionTime", actionTime == null ? "" : actionTime.toString(),
                        "reasonCode", reasonCode == null ? "" : reasonCode,
                        "partialDelivery", isPartialDelivery(payload)
                )
        ));

        return Map.of(
                "ok", true,
                "shipmentId", saved.getId(),
                "trackingCode", saved.getTrackingCode(),
                "status", saved.getStatus().value()
        );
    }

    private boolean shouldApplyOfficialStatus(ShipmentData shipment, ShipmentStatus nextStatus, Instant actionTime) {
        if (actionTime == null) {
            return false;
        }
        if (shipment.getStatus() == nextStatus) {
            return false;
        }
        if (actionTime != null && shipment.getLastOfficialEventAt() != null
                && !actionTime.isAfter(shipment.getLastOfficialEventAt())) {
            return false;
        }
        return ALLOWED_TRANSITIONS.getOrDefault(shipment.getStatus(), Set.of()).contains(nextStatus);
    }

    private ShipmentStatus officialStatus(int statusId) {
        return switch (statusId) {
            case -1 -> ShipmentStatus.CANCEL;
            case 1, 2, 12 -> ShipmentStatus.READY_TO_PICK;
            case 3 -> ShipmentStatus.PICKED;
            case 4 -> ShipmentStatus.DELIVERING;
            case 5 -> ShipmentStatus.DELIVERED;
            case 7 -> ShipmentStatus.PICKUP_FAILED;
            case 9 -> ShipmentStatus.DELIVERY_FAILED;
            case 20 -> ShipmentStatus.RETURNING;
            case 21 -> ShipmentStatus.RETURNED;
            default -> null;
        };
    }

    private String buildNote(int statusId, String reason) {
        String note = "Webhook GHTK: " + statusId + " - " + ghtkStatusLabel(statusId);
        return reason == null || reason.isBlank() ? note : note + " - " + reason.trim();
    }

    private String ghtkStatusLabel(int statusId) {
        return switch (statusId) {
            case -1 -> "Hủy đơn hàng";
            case 1 -> "Chưa tiếp nhận";
            case 2 -> "Đã tiếp nhận";
            case 3 -> "Đã lấy hàng/Đã nhập kho";
            case 4 -> "Đang giao hàng";
            case 5 -> "Đã giao hàng";
            case 6 -> "Đã đối soát";
            case 7 -> "Không lấy được hàng";
            case 8 -> "Delay lấy hàng";
            case 9 -> "Không giao được hàng";
            case 10 -> "Delay giao hàng";
            case 11 -> "Đối soát công nợ trả hàng";
            case 12 -> "Đang lấy hàng";
            case 13 -> "Đơn hàng bồi hoàn";
            case 20 -> "Đang trả hàng";
            case 21 -> "Đã trả hàng";
            case 45 -> "Shipper báo đã giao hàng";
            case 49 -> "Shipper báo không giao được";
            case 123 -> "Shipper báo đã lấy hàng";
            case 127 -> "Shipper báo không lấy được hàng";
            case 128 -> "Shipper báo delay lấy hàng";
            case 410 -> "Shipper báo delay giao hàng";
            default -> "Trạng thái không xác định";
        };
    }

    private String required(Map<String, Object> payload, String key) {
        String value = firstString(payload, key);
        if (value == null) {
            throw new BadRequestException("INVALID_GHTK_WEBHOOK", "GHTK webhook must include " + key);
        }
        return value;
    }

    private String firstString(Map<String, Object> payload, String key) {
        Object value = payload.get(key);
        if (value instanceof String text && !text.isBlank()) {
            return text.trim();
        }
        if (value instanceof Number number) {
            return String.valueOf(number);
        }
        return null;
    }

    private int parseStatusId(String rawStatus) {
        try {
            return Integer.parseInt(rawStatus);
        } catch (NumberFormatException ex) {
            throw new BadRequestException("INVALID_GHTK_STATUS", "GHTK status_id must be an integer");
        }
    }

    private Instant parseActionTime(String value) {
        if (value == null) {
            return null;
        }
        try {
            return OffsetDateTime.parse(value).toInstant();
        } catch (RuntimeException ex) {
            log.warn("Ignoring invalid GHTK action_time: {}", value);
            return null;
        }
    }

    private boolean isPartialDelivery(Map<String, Object> payload) {
        String value = firstString(payload, "return_part_package");
        return "1".equals(value);
    }

    private String serialize(Map<String, Object> payload) {
        try {
            return objectMapper.writeValueAsString(payload);
        } catch (JsonProcessingException ex) {
            throw new BadRequestException("INVALID_GHTK_WEBHOOK", "Cannot serialize GHTK webhook payload");
        }
    }

    private String eventKey(String trackingCode, String rawStatus, Instant actionTime) {
        String value = "ghtk|" + trackingCode + "|" + rawStatus + "|" + (actionTime == null ? "" : actionTime);
        try {
            byte[] digest = MessageDigest.getInstance("SHA-256").digest(value.getBytes(StandardCharsets.UTF_8));
            return java.util.HexFormat.of().formatHex(digest);
        } catch (java.security.NoSuchAlgorithmException ex) {
            throw new IllegalStateException("SHA-256 is unavailable", ex);
        }
    }
}
