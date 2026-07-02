package com.nitrotech.api.infrastructure.shipping.ghtk;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.nitrotech.api.infrastructure.shipping.ghtk.dto.GhtkOrderRequest;
import com.nitrotech.api.infrastructure.shipping.ghtk.dto.GhtkOrderResponse;
import com.nitrotech.api.shared.exception.ShippingException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.*;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

class GhtkClientTest {

    private GhtkClient ghtkClient;
    private MockRestServiceServer mockServer;
    private ObjectMapper objectMapper;

    @BeforeEach
    void setUp() {
        objectMapper = new ObjectMapper();
        RestClient.Builder builder = RestClient.builder();
        mockServer = MockRestServiceServer.bindTo(builder).build();
        ghtkClient = new GhtkClient(builder, "http://mock-ghtk.vn", "my-token", "test-client");
    }

    @Test
    void sendsRequestWithCorrectHeadersAndBody() throws Exception {
        GhtkOrderRequest request = GhtkOrderRequest.builder()
                .products(List.of())
                .order(GhtkOrderRequest.Order.builder().id("123").build())
                .build();

        GhtkOrderResponse expectedResponse = new GhtkOrderResponse();
        expectedResponse.setSuccess(true);
        expectedResponse.setMessage("success");

        mockServer.expect(requestTo("http://mock-ghtk.vn/services/shipment/order"))
                .andExpect(method(HttpMethod.POST))
                .andExpect(header("Token", "my-token"))
                .andExpect(header("X-Client-Source", "test-client"))
                .andExpect(header("Content-Type", MediaType.APPLICATION_JSON_VALUE))
                .andExpect(content().json(objectMapper.writeValueAsString(request)))
                .andRespond(withSuccess(objectMapper.writeValueAsString(expectedResponse), MediaType.APPLICATION_JSON));

        GhtkOrderResponse response = ghtkClient.createOrder(request);

        assertThat(response).isNotNull();
        assertThat(response.getSuccess()).isTrue();
        assertThat(response.getMessage()).isEqualTo("success");

        mockServer.verify();
    }

    @Test
    void rejectsCallsWhenTokenIsMissing() {
        GhtkClient clientWithoutToken = new GhtkClient(RestClient.builder(), "http://mock-ghtk.vn", "", "test-client");

        assertThatThrownBy(() -> clientWithoutToken.createOrder(
                GhtkOrderRequest.builder()
                        .products(List.of())
                        .order(GhtkOrderRequest.Order.builder().id("123").build())
                        .build()
        ))
                .isInstanceOf(ShippingException.class)
                .extracting("code").isEqualTo("GHTK_TOKEN_MISSING");
    }
}
