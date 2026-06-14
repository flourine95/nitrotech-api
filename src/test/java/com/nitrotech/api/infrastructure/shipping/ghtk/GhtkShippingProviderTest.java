package com.nitrotech.api.infrastructure.shipping.ghtk;

import com.nitrotech.api.domain.order.dto.OrderData;
import com.nitrotech.api.domain.order.dto.OrderItemData;
import com.nitrotech.api.domain.order.dto.ShippingAddressSnapshot;
import com.nitrotech.api.domain.shipping.dto.ShippingResult;
import com.nitrotech.api.infrastructure.shipping.ghtk.dto.GhtkOrderRequest;
import com.nitrotech.api.infrastructure.shipping.ghtk.dto.GhtkOrderResponse;
import com.nitrotech.api.shared.exception.ShippingException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.*;

class GhtkShippingProviderTest {

    private GhtkClient ghtkClient;
    private GhtkShippingProvider provider;

    @BeforeEach
    void setUp() {
        ghtkClient = mock(GhtkClient.class);
        provider = new GhtkShippingProvider(ghtkClient, new GhtkAddressNormalizer(),
                new GhtkPickupProperties(
                        "NitroTech Warehouse",
                        "0909000000",
                        null,
                        "590 CMT8 P.11",
                        "TP. Hồ Chí Minh",
                        "Quận 3",
                        "Phường 1"
                ));
    }

    @Test
    void mapsAndCreatesShipmentSuccessfullyForCod() {
        OrderData order = order("cod", new BigDecimal("500000"));
        GhtkOrderResponse.OrderDetails details = new GhtkOrderResponse.OrderDetails(
                "123", "S123.456", new BigDecimal("30000"), BigDecimal.ZERO, null, "2026-06-12 18:00:00", 2
        );
        GhtkOrderResponse response = new GhtkOrderResponse(true, "success", details);
        when(ghtkClient.createOrder(any())).thenReturn(response);

        ShippingResult result = provider.createShipment(order);

        assertThat(result).isNotNull();
        assertThat(result.getTrackingCode()).isEqualTo("S123.456");
        assertThat(result.getFee()).isEqualTo(new BigDecimal("30000"));
        assertThat(result.getEstimatedAt()).isNotNull();

        ArgumentCaptor<GhtkOrderRequest> requestCaptor = ArgumentCaptor.forClass(GhtkOrderRequest.class);
        verify(ghtkClient).createOrder(requestCaptor.capture());
        GhtkOrderRequest captured = requestCaptor.getValue();

        assertThat(captured.getProducts()).hasSize(1);
        assertThat(captured.getProducts().get(0).getName()).isEqualTo("Item A");
        assertThat(captured.getProducts().get(0).getWeight()).isEqualTo(0.2); // Default weight

        GhtkOrderRequest.Order capturedOrder = captured.getOrder();
        assertThat(capturedOrder.getId()).isEqualTo("123");
        assertThat(capturedOrder.getTel()).isEqualTo("0909123456");
        assertThat(capturedOrder.getPickName()).isEqualTo("NitroTech Warehouse");
        assertThat(capturedOrder.getPickTel()).isEqualTo("0909000000");
        assertThat(capturedOrder.getPickAddress()).isEqualTo("590 CMT8 P.11");
        assertThat(capturedOrder.getPickProvince()).isEqualTo("TP. Hồ Chí Minh");
        assertThat(capturedOrder.getPickDistrict()).isEqualTo("Quận 3");
        assertThat(capturedOrder.getPickWard()).isEqualTo("Phường 1");
        assertThat(capturedOrder.getPickMoney()).isEqualTo(new BigDecimal("500000")); // COD picks order total
        assertThat(capturedOrder.getIsFreeship()).isEqualTo(1);
        assertThat(capturedOrder.getProvince()).isEqualTo("TP. Hồ Chí Minh");
        assertThat(capturedOrder.getDistrict()).isEqualTo("Quận 1");
        assertThat(capturedOrder.getWard()).isEqualTo("Ben Nghe");
        assertThat(capturedOrder.getHamlet()).isEqualTo("Khác");
    }

    @Test
    void mapsAndCreatesShipmentSuccessfullyForPrepaid() {
        OrderData order = order("sepay", new BigDecimal("500000"));
        GhtkOrderResponse.OrderDetails details = new GhtkOrderResponse.OrderDetails(
                "123", "S123.456", new BigDecimal("30000"), BigDecimal.ZERO, null, "2026-06-12 18:00:00", 2
        );
        GhtkOrderResponse response = new GhtkOrderResponse(true, "success", details);
        when(ghtkClient.createOrder(any())).thenReturn(response);

        provider.createShipment(order);

        ArgumentCaptor<GhtkOrderRequest> requestCaptor = ArgumentCaptor.forClass(GhtkOrderRequest.class);
        verify(ghtkClient).createOrder(requestCaptor.capture());
        GhtkOrderRequest captured = requestCaptor.getValue();

        assertThat(captured.getOrder().getPickMoney()).isEqualTo(BigDecimal.ZERO); // Prepaid picks 0 COD
    }

    @Test
    void throwsShippingExceptionWhenGhtkThrowsException() {
        OrderData order = order("cod", new BigDecimal("500000"));
        when(ghtkClient.createOrder(any())).thenThrow(new RuntimeException("Connection timeout"));

        assertThatThrownBy(() -> provider.createShipment(order))
                .isInstanceOf(ShippingException.class)
                .hasMessageContaining("Failed to call GHTK API")
                .extracting("code").isEqualTo("GHTK_API_ERROR");
    }

    @Test
    void throwsShippingExceptionWhenGhtkReturnsFailure() {
        OrderData order = order("cod", new BigDecimal("500000"));
        GhtkOrderResponse response = new GhtkOrderResponse(false, "Address invalid", null);
        when(ghtkClient.createOrder(any())).thenReturn(response);

        assertThatThrownBy(() -> provider.createShipment(order))
                .isInstanceOf(ShippingException.class)
                .hasMessageContaining("GHTK failed to create order: Address invalid")
                .extracting("code").isEqualTo("GHTK_CREATION_FAILED");
    }

    @Test
    void throwsWhenPickupConfigIsMissing() {
        GhtkShippingProvider misconfigured = new GhtkShippingProvider(
                ghtkClient,
                new GhtkAddressNormalizer(),
                new GhtkPickupProperties(null, null, null, null, null, null, null)
        );

        assertThatThrownBy(() -> misconfigured.createShipment(order("cod", new BigDecimal("500000"))))
                .isInstanceOf(ShippingException.class)
                .hasMessageContaining("ghtk.pickup.name")
                .extracting("code").isEqualTo("GHTK_PICKUP_CONFIG_MISSING");
    }

    private OrderData order(String paymentMethod, BigDecimal finalAmount) {
        ShippingAddressSnapshot addr = new ShippingAddressSnapshot(
                "Nguyen Van A", "0909123456", "HCM", "79", "Q1", "760", "Ben Nghe", "20412", "123 Street"
        );
        OrderItemData item = new OrderItemData(
                1L, 10L, "Item A", "SKU-A", 1, new BigDecimal("500000"), new BigDecimal("500000")
        );
        return new OrderData(
                123L, 10L, addr, "confirmed", paymentMethod, finalAmount, BigDecimal.ZERO, BigDecimal.ZERO, finalAmount, null, "call first", List.of(item), Instant.now(), Instant.now()
        );
    }
}
