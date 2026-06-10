package com.nitrotech.api.infrastructure.shipping.ghn.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class GhnOrderResponse {
    private Integer code;
    private String message;
    private DataDetails data;

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class DataDetails {
        @JsonProperty("order_code")
        private String orderCode;

        @JsonProperty("total_fee")
        private Integer totalFee;

        @JsonProperty("expected_delivery_time")
        private String expectedDeliveryTime;
    }
}
