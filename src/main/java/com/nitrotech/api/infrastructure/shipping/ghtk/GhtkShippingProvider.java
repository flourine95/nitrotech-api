package com.nitrotech.api.infrastructure.shipping.ghtk;

import com.nitrotech.api.domain.order.dto.OrderData;
import com.nitrotech.api.domain.shipping.dto.ShippingResult;
import com.nitrotech.api.domain.shipping.provider.ShippingProvider;
import com.nitrotech.api.infrastructure.shipping.ghtk.dto.GhtkOrderRequest;
import com.nitrotech.api.infrastructure.shipping.ghtk.dto.GhtkOrderResponse;
import com.nitrotech.api.shared.exception.ShippingException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Locale;
import java.util.Map;

@Component
@RequiredArgsConstructor
public class GhtkShippingProvider implements ShippingProvider {

    private static final double DEFAULT_PRODUCT_WEIGHT = 0.2; // Default 200g in kg
    private static final String DEFAULT_HAMLET = "Khác";
    private static final Map<String, String> PROVINCE_ALIASES = Map.of(
            "hcm", "TP. Hồ Chí Minh",
            "tp hcm", "TP. Hồ Chí Minh",
            "tp. hcm", "TP. Hồ Chí Minh",
            "ho chi minh", "TP. Hồ Chí Minh",
            "hồ chí minh", "TP. Hồ Chí Minh",
            "tphcm", "TP. Hồ Chí Minh",
            "ha noi", "Hà Nội",
            "hà nội", "Hà Nội"
    );
    private static final Map<String, String> WARD_ALIASES = Map.of(
            "ben nghe", "Phường Bến Nghé",
            "bến nghé", "Phường Bến Nghé"
    );

    private final GhtkClient ghtkClient;

    @Override
    public String getProviderName() {
        return "ghtk";
    }

    @Override
    public ShippingResult createShipment(OrderData order) {
        GhtkOrderRequest request = mapToGhtkRequest(order);
        GhtkOrderResponse response;
        try {
            response = ghtkClient.createOrder(request);
        } catch (Exception e) {
            throw new ShippingException("GHTK_API_ERROR", "Failed to call GHTK API: " + e.getMessage());
        }

        if (response == null || !Boolean.TRUE.equals(response.getSuccess())) {
            String msg = (response != null) ? response.getMessage() : "Unknown error";
            throw new ShippingException("GHTK_CREATION_FAILED", "GHTK failed to create order: " + msg);
        }

        GhtkOrderResponse.OrderDetails details = response.getOrder();
        if (details == null) {
            throw new ShippingException("GHTK_RESPONSE_INVALID", "GHTK response is missing order details");
        }

        return ShippingResult.builder()
                .trackingCode(details.getLabel())
                .fee(details.getFee())
                .estimatedAt(parseEstimatedTime(details.getEstimatedDeliverTime()))
                .build();
    }

    private GhtkOrderRequest mapToGhtkRequest(OrderData order) {
        List<GhtkOrderRequest.Product> products = order.items().stream()
                .map(item -> GhtkOrderRequest.Product.builder()
                        .name(item.name())
                        .weight(DEFAULT_PRODUCT_WEIGHT)
                        .quantity(item.quantity())
                        .productCode(item.sku())
                        .price(item.unitPrice())
                        .build())
                .toList();

        var addr = order.shippingAddress();
        
        // GHTK is_freeship = 1 means Shop pays shipping fee (since Shop collects shipping fee from Buyer during checkout)
        int isFreeship = 1;

        // If payment method is COD, pick_money (amount to collect from buyer) is the finalAmount.
        // If prepaid (sepay, etc.), pick_money is 0.
        BigDecimal pickMoney = "cod".equalsIgnoreCase(order.paymentMethod()) 
                ? order.finalAmount() 
                : BigDecimal.ZERO;

        GhtkOrderRequest.Order ghtkOrder = GhtkOrderRequest.Order.builder()
                .id(order.id().toString())
                .tel(addr.phone())
                .name(addr.receiver())
                .address(normalizeAddress(addr.street()))
                .province(normalizeProvince(addr.province()))
                .district(normalizeDistrict(addr.district()))
                .ward(normalizeWard(addr.ward()))
                .hamlet(DEFAULT_HAMLET)
                .isFreeship(isFreeship)
                .pickMoney(pickMoney)
                .note(order.note())
                .value(order.finalAmount()) // declared value for package insurance
                .build();

        return GhtkOrderRequest.builder()
                .products(products)
                .order(ghtkOrder)
                .build();
    }

    private Instant parseEstimatedTime(String timeStr) {
        if (timeStr == null || timeStr.isBlank()) {
            return null;
        }
        try {
            // GHTK typically returns date format like "yyyy-MM-dd HH:mm:ss" or similar in Asia/Ho_Chi_Minh timezone
            DateTimeFormatter formatter = DateTimeFormatter
                    .ofPattern("yyyy-MM-dd HH:mm:ss")
                    .withZone(ZoneId.of("Asia/Ho_Chi_Minh"));
            return Instant.from(formatter.parse(timeStr.trim()));
        } catch (Exception e) {
            // Fallback to 3 days from now if parsing fails
            return Instant.now().plus(Duration.ofDays(3));
        }
    }

    private String normalizeAddress(String value) {
        return normalizeWhitespace(value);
    }

    private String normalizeProvince(String value) {
        String normalized = normalizeWhitespace(value);
        if (normalized == null) {
            return null;
        }
        return PROVINCE_ALIASES.getOrDefault(key(normalized), normalized);
    }

    private String normalizeDistrict(String value) {
        String normalized = normalizeWhitespace(value);
        if (normalized == null) {
            return null;
        }
        String key = key(normalized);
        if (key.matches("q\\d+")) {
            return "Quận " + key.substring(1);
        }
        if (key.matches("quan\\s*\\d+")) {
            return "Quận " + key.replaceAll("\\D+", "");
        }
        return normalized;
    }

    private String normalizeWard(String value) {
        String normalized = normalizeWhitespace(value);
        if (normalized == null) {
            return null;
        }
        String key = key(normalized);
        if (WARD_ALIASES.containsKey(key)) {
            return WARD_ALIASES.get(key);
        }
        if (key.matches("p\\d+")) {
            return "Phường " + key.substring(1);
        }
        if (key.matches("phuong\\s*\\d+")) {
            return "Phường " + key.replaceAll("\\D+", "");
        }
        return normalized;
    }

    private String normalizeWhitespace(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        return value.trim().replaceAll("\\s+", " ");
    }

    private String key(String value) {
        return normalizeWhitespace(value)
                .toLowerCase(Locale.ROOT)
                .replace(".", "");
    }
}
