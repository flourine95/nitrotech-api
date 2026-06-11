package com.nitrotech.api.infrastructure.shipping.ghn;

import com.nitrotech.api.infrastructure.shipping.ghn.dto.GhnDistrictResponse;
import com.nitrotech.api.infrastructure.shipping.ghn.dto.GhnOrderRequest;
import com.nitrotech.api.infrastructure.shipping.ghn.dto.GhnOrderResponse;
import com.nitrotech.api.infrastructure.shipping.ghn.dto.GhnProvinceResponse;
import com.nitrotech.api.infrastructure.shipping.ghn.dto.GhnWardResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import java.util.Map;

@Component
public class GhnClient {

    private final RestClient restClient;

    public GhnClient(
            RestClient.Builder builder,
            @Value("${ghn.api-url}") String apiUrl,
            @Value("${ghn.token}") String token,
            @Value("${ghn.shop-id}") String shopId
    ) {
        this.restClient = builder
                .baseUrl(apiUrl)
                .defaultHeader("Token", token)
                .defaultHeader("ShopId", shopId)
                .defaultHeader("Content-Type", MediaType.APPLICATION_JSON_VALUE)
                .build();
    }

    public GhnProvinceResponse getProvinces() {
        return restClient.get()
                .uri("/master-data/province")
                .retrieve()
                .body(GhnProvinceResponse.class);
    }

    public GhnDistrictResponse getDistricts(Integer provinceId) {
        return restClient.post()
                .uri("/master-data/district")
                .body(Map.of("province_id", provinceId))
                .retrieve()
                .body(GhnDistrictResponse.class);
    }

    public GhnWardResponse getWards(Integer districtId) {
        return restClient.post()
                .uri("/master-data/ward")
                .body(Map.of("district_id", districtId))
                .retrieve()
                .body(GhnWardResponse.class);
    }

    public GhnOrderResponse createOrder(GhnOrderRequest request) {
        return restClient.post()
                .uri("/v2/shipping-order/create")
                .body(request)
                .retrieve()
                .body(GhnOrderResponse.class);
    }
}
