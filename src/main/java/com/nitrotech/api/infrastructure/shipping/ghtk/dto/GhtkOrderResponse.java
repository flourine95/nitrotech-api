package com.nitrotech.api.infrastructure.shipping.ghtk.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class GhtkOrderResponse {
    private Boolean success;
    private String message;
    private OrderDetails order;

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class OrderDetails {
        @JsonProperty("partner_id")
        private String partnerId;
        private String label; // GHTK tracking code (mã vận đơn)
        private BigDecimal fee;
        @JsonProperty("insurance_fee")
        private BigDecimal insuranceFee;
        @JsonProperty("estimated_pick_time")
        private String estimatedPickTime;
        @JsonProperty("estimated_deliver_time")
        private String estimatedDeliverTime;
        @JsonProperty("status_id")
        private Integer statusId;
    }
}
