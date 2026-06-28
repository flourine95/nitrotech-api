package com.nitrotech.api.infrastructure.shipping.ghtk.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

import java.math.BigDecimal;

@Data
public class GhtkFeeResponse {
    private Boolean success;
    private String message;
    private Fee fee;

    @Data
    public static class Fee {
        private BigDecimal fee;
        @JsonProperty("insurance_fee")
        private BigDecimal insuranceFee;
        private Boolean delivery;
    }
}
