package com.nitrotech.api.infrastructure.shipping.ghtk;

import com.nitrotech.api.infrastructure.shipping.ghtk.dto.GhtkOrderRequest;
import com.nitrotech.api.infrastructure.shipping.ghtk.dto.GhtkOrderResponse;
import com.nitrotech.api.infrastructure.shipping.ghtk.dto.GhtkFeeResponse;
import com.nitrotech.api.shared.exception.ShippingException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClientResponseException;
import org.springframework.web.client.RestClient;

import java.math.BigDecimal;

@Component
public class GhtkClient {

    private final RestClient restClient;
    private final String token;

    public GhtkClient(
            RestClient.Builder builder,
            @Value("${ghtk.api-url}") String apiUrl,
            @Value("${ghtk.token}") String token,
            @Value("${ghtk.client-source:}") String clientSource
    ) {
        this.token = token == null ? "" : token.trim();
        this.restClient = builder
                .baseUrl(apiUrl)
                .defaultHeader("Token", this.token)
                .defaultHeader("X-Client-Source", clientSource)
                .defaultHeader("Content-Type", MediaType.APPLICATION_JSON_VALUE)
                .build();
    }

    public GhtkOrderResponse createOrder(GhtkOrderRequest request) {
        ensureTokenConfigured();
        try {
            return restClient.post()
                    .uri("/services/shipment/order")
                    .body(request)
                    .retrieve()
                    .body(GhtkOrderResponse.class);
        } catch (RestClientResponseException ex) {
            throw authOrTransportError("GHTK_API_ERROR", "Failed to call GHTK API", ex);
        }
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
        ensureTokenConfigured();
        try {
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
        } catch (RestClientResponseException ex) {
            throw authOrTransportError("GHTK_FEE_API_ERROR", "Failed to call GHTK fee API", ex);
        }
    }

    private void ensureTokenConfigured() {
        if (token.isBlank()) {
            throw new ShippingException(
                    "GHTK_TOKEN_MISSING",
                    "GHTK token is required when shipment simulation is disabled"
            );
        }
    }

    private ShippingException authOrTransportError(String code, String prefix, RestClientResponseException ex) {
        if (ex.getStatusCode().value() == 401 || ex.getStatusCode().value() == 403) {
            return new ShippingException(
                    "GHTK_AUTH_INVALID",
                    "GHTK token is missing or invalid"
            );
        }
        return new ShippingException(code, prefix + ": " + ex.getMessage());
    }
}
