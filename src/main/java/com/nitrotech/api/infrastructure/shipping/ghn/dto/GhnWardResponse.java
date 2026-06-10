package com.nitrotech.api.infrastructure.shipping.ghn.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class GhnWardResponse {
    private Integer code;
    private String message;
    private List<WardData> data;

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class WardData {
        @JsonProperty("WardCode")
        private String wardCode;

        @JsonProperty("DistrictID")
        private Integer districtID;

        @JsonProperty("WardName")
        private String wardName;
    }
}
