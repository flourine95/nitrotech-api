package com.nitrotech.api.infrastructure.shipping.ghn;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.nitrotech.api.infrastructure.shipping.ghn.dto.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.*;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

class GhnClientTest {

    private GhnClient ghnClient;
    private MockRestServiceServer mockServer;
    private ObjectMapper objectMapper;

    @BeforeEach
    void setUp() {
        objectMapper = new ObjectMapper();
        RestClient.Builder builder = RestClient.builder();
        mockServer = MockRestServiceServer.bindTo(builder).build();
        ghnClient = new GhnClient(builder, "http://mock-ghn.vn", "my-token", "123456");
    }

    @Test
    void getProvincesSendsGetRequest() throws Exception {
        GhnProvinceResponse expectedResponse = new GhnProvinceResponse(
                200,
                "Success",
                List.of(new GhnProvinceResponse.ProvinceData(1, "Hồ Chí Minh", "79"))
        );

        mockServer.expect(requestTo("http://mock-ghn.vn/master-data/province"))
                .andExpect(method(HttpMethod.GET))
                .andExpect(header("Token", "my-token"))
                .andExpect(header("ShopId", "123456"))
                .andExpect(header("Content-Type", MediaType.APPLICATION_JSON_VALUE))
                .andRespond(withSuccess(objectMapper.writeValueAsString(expectedResponse), MediaType.APPLICATION_JSON));

        GhnProvinceResponse response = ghnClient.getProvinces();

        assertThat(response).isNotNull();
        assertThat(response.getCode()).isEqualTo(200);
        assertThat(response.getData()).hasSize(1);
        assertThat(response.getData().get(0).getProvinceName()).isEqualTo("Hồ Chí Minh");

        mockServer.verify();
    }

    @Test
    void getDistrictsSendsPostRequest() throws Exception {
        GhnDistrictResponse expectedResponse = new GhnDistrictResponse(
                200,
                "Success",
                List.of(new GhnDistrictResponse.DistrictData(12, 1, "Quận 1", "760"))
        );

        mockServer.expect(requestTo("http://mock-ghn.vn/master-data/district"))
                .andExpect(method(HttpMethod.POST))
                .andExpect(header("Token", "my-token"))
                .andExpect(header("ShopId", "123456"))
                .andExpect(header("Content-Type", MediaType.APPLICATION_JSON_VALUE))
                .andExpect(content().json(objectMapper.writeValueAsString(Map.of("province_id", 1))))
                .andRespond(withSuccess(objectMapper.writeValueAsString(expectedResponse), MediaType.APPLICATION_JSON));

        GhnDistrictResponse response = ghnClient.getDistricts(1);

        assertThat(response).isNotNull();
        assertThat(response.getCode()).isEqualTo(200);
        assertThat(response.getData()).hasSize(1);
        assertThat(response.getData().get(0).getDistrictName()).isEqualTo("Quận 1");

        mockServer.verify();
    }

    @Test
    void getWardsSendsPostRequest() throws Exception {
        GhnWardResponse expectedResponse = new GhnWardResponse(
                200,
                "Success",
                List.of(new GhnWardResponse.WardData("W123", 12, "Phường Bến Nghé"))
        );

        mockServer.expect(requestTo("http://mock-ghn.vn/master-data/ward"))
                .andExpect(method(HttpMethod.POST))
                .andExpect(header("Token", "my-token"))
                .andExpect(header("ShopId", "123456"))
                .andExpect(header("Content-Type", MediaType.APPLICATION_JSON_VALUE))
                .andExpect(content().json(objectMapper.writeValueAsString(Map.of("district_id", 12))))
                .andRespond(withSuccess(objectMapper.writeValueAsString(expectedResponse), MediaType.APPLICATION_JSON));

        GhnWardResponse response = ghnClient.getWards(12);

        assertThat(response).isNotNull();
        assertThat(response.getCode()).isEqualTo(200);
        assertThat(response.getData()).hasSize(1);
        assertThat(response.getData().get(0).getWardName()).isEqualTo("Phường Bến Nghé");

        mockServer.verify();
    }

    @Test
    void createOrderSendsPostRequest() throws Exception {
        GhnOrderRequest request = GhnOrderRequest.builder()
                .toName("Nguyen Van A")
                .toPhone("0909123456")
                .toAddress("123 Street")
                .toDistrictId(12)
                .toWardCode("W123")
                .codAmount(100000)
                .build();

        GhnOrderResponse expectedResponse = new GhnOrderResponse(
                200,
                "Success",
                new GhnOrderResponse.DataDetails("GHN-ABC", 25000, "2026-06-14T18:00:00Z")
        );

        mockServer.expect(requestTo("http://mock-ghn.vn/v2/shipping-order/create"))
                .andExpect(method(HttpMethod.POST))
                .andExpect(header("Token", "my-token"))
                .andExpect(header("ShopId", "123456"))
                .andExpect(header("Content-Type", MediaType.APPLICATION_JSON_VALUE))
                .andExpect(content().json(objectMapper.writeValueAsString(request)))
                .andRespond(withSuccess(objectMapper.writeValueAsString(expectedResponse), MediaType.APPLICATION_JSON));

        GhnOrderResponse response = ghnClient.createOrder(request);

        assertThat(response).isNotNull();
        assertThat(response.getCode()).isEqualTo(200);
        assertThat(response.getData().getOrderCode()).isEqualTo("GHN-ABC");
        assertThat(response.getData().getTotalFee()).isEqualTo(25000);

        mockServer.verify();
    }
}
