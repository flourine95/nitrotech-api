package com.nitrotech.api.infrastructure.shipping.ghtk;

import com.nitrotech.api.domain.order.dto.OrderData;
import com.nitrotech.api.domain.order.dto.OrderItemData;
import com.nitrotech.api.domain.payment.PaymentMethod;
import com.nitrotech.api.domain.shipping.dto.ShippingFeeQuote;
import com.nitrotech.api.domain.shipping.dto.ShippingFeeQuoteRequest;
import com.nitrotech.api.domain.shipping.dto.ShippingResult;
import com.nitrotech.api.domain.shipping.provider.ShippingProvider;
import com.nitrotech.api.infrastructure.shipping.ghtk.dto.GhtkFeeResponse;
import com.nitrotech.api.infrastructure.shipping.ghtk.dto.GhtkOrderRequest;
import com.nitrotech.api.infrastructure.shipping.ghtk.dto.GhtkOrderResponse;
import com.nitrotech.api.shared.exception.ShippingException;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.List;

@Component
@RequiredArgsConstructor
@EnableConfigurationProperties(GhtkPickupProperties.class)
public class GhtkShippingProvider implements ShippingProvider {

    private final GhtkClient ghtkClient;
    private final GhtkAddressNormalizer addressNormalizer;
    private final GhtkPickupProperties pickupProperties;

    @Value("${app.shipping.default-weight-grams:1000}")
    private int defaultWeightGrams;

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

    @Override
    public ShippingFeeQuote quoteFee(ShippingFeeQuoteRequest request) {
        var addr = request.shippingAddress();
        GhtkFeeResponse response;
        try {
            response = ghtkClient.calculateFee(
                    requiredPickup("address", pickupProperties.address()),
                    addressNormalizer.normalizeProvince(requiredPickup("province", pickupProperties.province())),
                    addressNormalizer.normalizeDistrict(requiredPickup("district", pickupProperties.district())),
                    addressNormalizer.normalizeWard(pickupProperties.ward()),
                    addressNormalizer.normalizeAddress(addr.street()),
                    addressNormalizer.normalizeProvince(addr.province()),
                    addressNormalizer.normalizeDistrict(addr.district()),
                    addressNormalizer.normalizeWard(addr.ward()),
                    totalWeightGrams(request.items()),
                    request.orderValue()
            );
        } catch (Exception e) {
            throw new ShippingException("GHTK_FEE_API_ERROR", "Failed to call GHTK fee API: " + e.getMessage());
        }

        if (response == null || !Boolean.TRUE.equals(response.getSuccess()) || response.getFee() == null) {
            String msg = response == null ? "Unknown error" : response.getMessage();
            throw new ShippingException("GHTK_FEE_FAILED", "GHTK failed to quote fee: " + msg);
        }

        return new ShippingFeeQuote(
                response.getFee().getFee(),
                response.getFee().getInsuranceFee(),
                Boolean.TRUE.equals(response.getFee().getDelivery())
        );
    }

    private GhtkOrderRequest mapToGhtkRequest(OrderData order) {
        List<GhtkOrderRequest.Product> products = order.items().stream()
                .map(item -> GhtkOrderRequest.Product.builder()
                        .name(item.name())
                        .weight(weightKg(item))
                        .quantity(item.quantity())
                        .productCode(item.sku())
                        .price(item.unitPrice())
                        .height(item.heightCm())
                        .width(item.widthCm())
                        .length(item.lengthCm())
                        .build())
                .toList();

        var addr = order.shippingAddress();
        
        // GHTK is_freeship = 1 means Shop pays shipping fee (since Shop collects shipping fee from Buyer during checkout)
        int isFreeship = 1;

        // If payment method is COD, pick_money (amount to collect from buyer) is the finalAmount.
        // If prepaid (sepay, etc.), pick_money is 0.
        BigDecimal pickMoney = PaymentMethod.COD.value().equalsIgnoreCase(order.paymentMethod())
                ? order.finalAmount() 
                : BigDecimal.ZERO;

        GhtkOrderRequest.Order ghtkOrder = GhtkOrderRequest.Order.builder()
                .id(order.id().toString())
                .pickName(requiredPickup("name", pickupProperties.name()))
                .pickAddressId(pickupProperties.addressId())
                .pickAddress(requiredPickup("address", pickupProperties.address()))
                .pickProvince(addressNormalizer.normalizeProvince(requiredPickup("province", pickupProperties.province())))
                .pickDistrict(addressNormalizer.normalizeDistrict(requiredPickup("district", pickupProperties.district())))
                .pickWard(addressNormalizer.normalizeWard(pickupProperties.ward()))
                .pickTel(requiredPickup("tel", pickupProperties.tel()))
                .tel(addr.phone())
                .name(addr.receiver())
                .address(addressNormalizer.normalizeAddress(addr.street()))
                .province(addressNormalizer.normalizeProvince(addr.province()))
                .district(addressNormalizer.normalizeDistrict(addr.district()))
                .ward(addressNormalizer.normalizeWard(addr.ward()))
                .hamlet(GhtkAddressNormalizer.DEFAULT_HAMLET)
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

    private int totalWeightGrams(List<OrderItemData> items) {
        return items.stream()
                .mapToInt(item -> weightGrams(item) * item.quantity())
                .sum();
    }

    private int weightGrams(OrderItemData item) {
        return item.weightGrams() == null ? defaultWeightGrams : item.weightGrams();
    }

    private double weightKg(OrderItemData item) {
        return weightGrams(item) / 1000.0;
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

    private String requiredPickup(String field, String value) {
        if (value == null || value.isBlank()) {
            throw new ShippingException("GHTK_PICKUP_CONFIG_MISSING",
                    "Missing GHTK pickup config: ghtk.pickup." + field);
        }
        return value.trim();
    }
}
