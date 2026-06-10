package com.nitrotech.api.infrastructure.shipping.ghtk;

import com.nitrotech.api.infrastructure.shipping.ghtk.dto.GhtkOrderRequest;
import com.nitrotech.api.infrastructure.shipping.ghtk.dto.GhtkOrderResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

@Component
public class GhtkClient {

    private final RestClient restClient;

    public GhtkClient(
            RestClient.Builder builder,
            @Value("${ghtk.api-url}") String apiUrl,
            @Value("${ghtk.token}") String token
    ) {
        this.restClient = builder
                .baseUrl(apiUrl)
                .defaultHeader("Token", token)
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
}
