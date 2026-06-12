package com.nitrotech.api.infrastructure.shipping.ghn;

import com.nitrotech.api.domain.order.dto.OrderData;
import com.nitrotech.api.domain.shipping.dto.ShippingResult;
import com.nitrotech.api.domain.shipping.provider.ShippingProvider;
import com.nitrotech.api.infrastructure.shipping.ghn.dto.GhnOrderRequest;
import com.nitrotech.api.infrastructure.shipping.ghn.dto.GhnOrderResponse;
import com.nitrotech.api.shared.exception.ShippingException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.List;

@Slf4j
@Component
@RequiredArgsConstructor
public class GhnShippingProvider implements ShippingProvider {

    private static final int DEFAULT_PRODUCT_WEIGHT_G = 200; // 200g
    private static final int DEFAULT_DIMENSION_CM = 10;
    
    private final GhnClient ghnClient;
    private final GhnCarrierAddressResolver ghnCarrierAddressResolver;

    @Override
    public String getProviderName() {
        return "ghn";
    }

    @Override
    public ShippingResult createShipment(OrderData order) {
        log.info("Creating GHN shipment for orderId: {}", order.id());
        GhnOrderRequest request = mapToGhnRequest(order);
        GhnOrderResponse response;
        try {
            response = ghnClient.createOrder(request);
        } catch (Exception e) {
            throw new ShippingException("GHN_API_ERROR", "Failed to call GHN API: " + e.getMessage());
        }

        if (response == null || response.getCode() == null || response.getCode() != 200) {
            String msg = (response != null) ? response.getMessage() : "Unknown error";
            throw new ShippingException("GHN_CREATION_FAILED", "GHN failed to create order: " + msg);
        }

        GhnOrderResponse.DataDetails data = response.getData();
        if (data == null) {
            throw new ShippingException("GHN_RESPONSE_INVALID", "GHN response is missing data details");
        }

        BigDecimal fee = data.getTotalFee() != null ? new BigDecimal(data.getTotalFee()) : BigDecimal.ZERO;

        return ShippingResult.builder()
                .trackingCode(data.getOrderCode())
                .fee(fee)
                .estimatedAt(parseEstimatedTime(data.getExpectedDeliveryTime()))
                .build();
    }

    private GhnOrderRequest mapToGhnRequest(OrderData order) {
        var addr = order.shippingAddress();
        if (addr == null) {
            throw new ShippingException("INVALID_ADDRESS_CODE", "Shipping address is missing");
        }

        // 1. Resolve internal address codes into GHN specific IDs/codes
        GhnCarrierAddress carrierAddress = ghnCarrierAddressResolver.resolve(addr);

        // 2. Map items and calculate total weight
        List<GhnOrderRequest.Item> items = order.items().stream()
                .map(item -> GhnOrderRequest.Item.builder()
                        .name(item.name())
                        .code(item.sku())
                        .quantity(item.quantity())
                        .price(item.unitPrice().intValue())
                        .build())
                .toList();

        int totalWeightG = order.items().stream()
                .mapToInt(item -> DEFAULT_PRODUCT_WEIGHT_G * item.quantity())
                .sum();

        // 3. Map payment details (1: Shop pays shipping fee)
        int paymentTypeId = 1;

        // If payment method is COD, collect order final amount. Otherwise (prepaid), collect 0.
        int codAmount = "cod".equalsIgnoreCase(order.paymentMethod()) 
                ? order.finalAmount().intValue() 
                : 0;

        // Declared value for package insurance, capped at 20,000,000 VND
        int insuranceValue = Math.min(order.finalAmount().intValue(), 20_000_000);

        return GhnOrderRequest.builder()
                .paymentTypeId(paymentTypeId)
                .note(order.note())
                .requiredNote("KHONGCHOXEMHANG")
                .toName(addr.receiver())
                .toPhone(addr.phone())
                .toAddress(addr.street())
                .toWardCode(carrierAddress.wardCode())
                .toDistrictId(carrierAddress.districtId())
                .codAmount(codAmount)
                .weight(totalWeightG)
                .length(DEFAULT_DIMENSION_CM)
                .width(DEFAULT_DIMENSION_CM)
                .height(DEFAULT_DIMENSION_CM)
                .serviceTypeId(2) // Standard shipping
                .insuranceValue(insuranceValue)
                .items(items)
                .build();
    }

    private Instant parseEstimatedTime(String timeStr) {
        if (timeStr == null || timeStr.isBlank()) {
            return null;
        }
        try {
            return Instant.parse(timeStr.trim());
        } catch (Exception e) {
            try {
                LocalDateTime ldt = LocalDateTime.parse(timeStr.trim());
                return ldt.atZone(ZoneId.of("Asia/Ho_Chi_Minh")).toInstant();
            } catch (Exception ex) {
                return Instant.now().plus(Duration.ofDays(3));
            }
        }
    }
}
