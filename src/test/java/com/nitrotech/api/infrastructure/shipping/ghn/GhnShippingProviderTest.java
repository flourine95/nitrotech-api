package com.nitrotech.api.infrastructure.shipping.ghn;

import com.nitrotech.api.domain.order.dto.OrderData;
import com.nitrotech.api.domain.order.dto.OrderItemData;
import com.nitrotech.api.domain.order.dto.ShippingAddressSnapshot;
import com.nitrotech.api.domain.shipping.dto.ShippingResult;
import com.nitrotech.api.infrastructure.shipping.ghn.dto.GhnOrderRequest;
import com.nitrotech.api.infrastructure.shipping.ghn.dto.GhnOrderResponse;
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

class GhnShippingProviderTest {

    private GhnClient ghnClient;
    private GhnAddressResolver ghnAddressResolver;
    private GhnShippingProvider provider;

    @BeforeEach
    void setUp() {
        ghnClient = mock(GhnClient.class);
        ghnAddressResolver = mock(GhnAddressResolver.class);
        provider = new GhnShippingProvider(ghnClient, ghnAddressResolver);
    }

    @Test
    void mapsAndCreatesShipmentSuccessfullyForCod() {
        OrderData order = order("cod", new BigDecimal("500000"));
        
        when(ghnAddressResolver.getProvinceId("79", "HCM")).thenReturn(10);
        when(ghnAddressResolver.getDistrictId(10, "760", "Q1")).thenReturn(101);
        when(ghnAddressResolver.getWardCode(101, "Ben Nghe")).thenReturn("W_BEN_NGHE");

        GhnOrderResponse.DataDetails details = new GhnOrderResponse.DataDetails(
                "GHN123456", 25000, "2026-06-14T18:00:00Z"
        );
        GhnOrderResponse response = new GhnOrderResponse(200, "Success", details);
        when(ghnClient.createOrder(any())).thenReturn(response);

        ShippingResult result = provider.createShipment(order);

        assertThat(result).isNotNull();
        assertThat(result.getTrackingCode()).isEqualTo("GHN123456");
        assertThat(result.getFee()).isEqualTo(new BigDecimal("25000"));
        assertThat(result.getEstimatedAt()).isNotNull();

        ArgumentCaptor<GhnOrderRequest> requestCaptor = ArgumentCaptor.forClass(GhnOrderRequest.class);
        verify(ghnClient).createOrder(requestCaptor.capture());
        GhnOrderRequest captured = requestCaptor.getValue();

        assertThat(captured.getToName()).isEqualTo("Nguyen Van A");
        assertThat(captured.getToPhone()).isEqualTo("0909123456");
        assertThat(captured.getToAddress()).isEqualTo("123 Street");
        assertThat(captured.getToWardCode()).isEqualTo("W_BEN_NGHE");
        assertThat(captured.getToDistrictId()).isEqualTo(101);
        assertThat(captured.getCodAmount()).isEqualTo(500000); // COD amount matches finalAmount
        assertThat(captured.getInsuranceValue()).isEqualTo(500000); // Insurance matches finalAmount
        assertThat(captured.getWeight()).isEqualTo(200); // 1 item * 200g
        assertThat(captured.getItems()).hasSize(1);
        assertThat(captured.getItems().get(0).getName()).isEqualTo("Item A");
        assertThat(captured.getItems().get(0).getQuantity()).isEqualTo(1);
    }

    @Test
    void mapsAndCreatesShipmentSuccessfullyForPrepaid() {
        OrderData order = order("sepay", new BigDecimal("500000"));
        
        when(ghnAddressResolver.getProvinceId("79", "HCM")).thenReturn(10);
        when(ghnAddressResolver.getDistrictId(10, "760", "Q1")).thenReturn(101);
        when(ghnAddressResolver.getWardCode(101, "Ben Nghe")).thenReturn("W_BEN_NGHE");

        GhnOrderResponse.DataDetails details = new GhnOrderResponse.DataDetails(
                "GHN123456", 25000, "2026-06-14T18:00:00Z"
        );
        GhnOrderResponse response = new GhnOrderResponse(200, "Success", details);
        when(ghnClient.createOrder(any())).thenReturn(response);

        provider.createShipment(order);

        ArgumentCaptor<GhnOrderRequest> requestCaptor = ArgumentCaptor.forClass(GhnOrderRequest.class);
        verify(ghnClient).createOrder(requestCaptor.capture());
        GhnOrderRequest captured = requestCaptor.getValue();

        assertThat(captured.getCodAmount()).isEqualTo(0); // Prepaid -> 0 COD
        assertThat(captured.getInsuranceValue()).isEqualTo(500000); // Still insured
    }

    @Test
    void capsInsuranceValueAt20Million() {
        OrderData order = order("cod", new BigDecimal("25000000")); // 25 Million VND
        
        when(ghnAddressResolver.getProvinceId("79", "HCM")).thenReturn(10);
        when(ghnAddressResolver.getDistrictId(10, "760", "Q1")).thenReturn(101);
        when(ghnAddressResolver.getWardCode(101, "Ben Nghe")).thenReturn("W_BEN_NGHE");

        GhnOrderResponse.DataDetails details = new GhnOrderResponse.DataDetails(
                "GHN123456", 25000, "2026-06-14T18:00:00Z"
        );
        GhnOrderResponse response = new GhnOrderResponse(200, "Success", details);
        when(ghnClient.createOrder(any())).thenReturn(response);

        provider.createShipment(order);

        ArgumentCaptor<GhnOrderRequest> requestCaptor = ArgumentCaptor.forClass(GhnOrderRequest.class);
        verify(ghnClient).createOrder(requestCaptor.capture());
        GhnOrderRequest captured = requestCaptor.getValue();

        assertThat(captured.getInsuranceValue()).isEqualTo(20000000); // Capped at 20 Million
    }

    @Test
    void throwsShippingExceptionWhenGhnClientThrowsException() {
        OrderData order = order("cod", new BigDecimal("500000"));

        when(ghnAddressResolver.getProvinceId("79", "HCM")).thenReturn(10);
        when(ghnAddressResolver.getDistrictId(10, "760", "Q1")).thenReturn(101);
        when(ghnAddressResolver.getWardCode(101, "Ben Nghe")).thenReturn("W_BEN_NGHE");

        when(ghnClient.createOrder(any())).thenThrow(new RuntimeException("Connection timeout"));

        assertThatThrownBy(() -> provider.createShipment(order))
                .isInstanceOf(ShippingException.class)
                .hasMessageContaining("Failed to call GHN API: Connection timeout")
                .extracting("code").isEqualTo("GHN_API_ERROR");
    }

    @Test
    void throwsShippingExceptionWhenGhnReturnsFailureCode() {
        OrderData order = order("cod", new BigDecimal("500000"));

        when(ghnAddressResolver.getProvinceId("79", "HCM")).thenReturn(10);
        when(ghnAddressResolver.getDistrictId(10, "760", "Q1")).thenReturn(101);
        when(ghnAddressResolver.getWardCode(101, "Ben Nghe")).thenReturn("W_BEN_NGHE");

        GhnOrderResponse response = new GhnOrderResponse(400, "District ID invalid", null);
        when(ghnClient.createOrder(any())).thenReturn(response);

        assertThatThrownBy(() -> provider.createShipment(order))
                .isInstanceOf(ShippingException.class)
                .hasMessageContaining("GHN failed to create order: District ID invalid")
                .extracting("code").isEqualTo("GHN_CREATION_FAILED");
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
