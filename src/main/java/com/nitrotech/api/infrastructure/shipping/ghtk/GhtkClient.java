package com.nitrotech.api.infrastructure.shipping.ghtk;

import com.nitrotech.api.infrastructure.shipping.ghtk.dto.GhtkOrderRequest;
import com.nitrotech.api.infrastructure.shipping.ghtk.dto.GhtkOrderResponse;
import com.nitrotech.api.infrastructure.shipping.ghtk.dto.GhtkFeeResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import java.math.BigDecimal;

@Component
public class GhtkClient {

    private final RestClient restClient;

    public GhtkClient(
            RestClient.Builder builder,
            @Value("${ghtk.api-url}") String apiUrl,
            @Value("${ghtk.token}") String token,
            @Value("${ghtk.client-source:}") String clientSource
    ) {
        this.restClient = builder
                .baseUrl(apiUrl)
                .defaultHeader("Token", token)
                .defaultHeader("X-Client-Source", clientSource)
                .defaultHeader("Content-Type", MediaType.APPLICATION_JSON_VALUE)
                .build();
    }

    public GhtkOrderResponse createOrder(GhtkOrderRequest request) {
        return restClient.post()
                .uri("/services/shipment/order")
                .body(request)
                .retrieve()
                .body(GhtkOrderResponse.class);
    }

    public GhtkFeeResponse calculateFee(
            String pickAddress,
            String pickProvince,
            String pickDistrict,
            String pickWard,
            String address,
            String province,
            String district,
            String ward,
            int weightGrams,
            BigDecimal value
    ) {
        return restClient.get()
                .uri(uri -> uri.path("/services/shipment/fee")
                        .queryParam("pick_address", pickAddress)
                        .queryParam("pick_province", pickProvince)
                        .queryParam("pick_district", pickDistrict)
                        .queryParamIfPresent("pick_ward", java.util.Optional.ofNullable(pickWard))
                        .queryParam("address", address)
                        .queryParam("province", province)
                        .queryParam("district", district)
                        .queryParamIfPresent("ward", java.util.Optional.ofNullable(ward))
                        .queryParam("weight", weightGrams)
                        .queryParam("value", value.toBigInteger())
                        .build())
                .retrieve()
                .body(GhtkFeeResponse.class);
    }
}
