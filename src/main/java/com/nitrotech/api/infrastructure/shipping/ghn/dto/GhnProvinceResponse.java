package com.nitrotech.api.infrastructure.shipping.ghn.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class GhnProvinceResponse {
    private Integer code;
    private String message;
    private List<ProvinceData> data;

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ProvinceData {
        @JsonProperty("ProvinceID")
        private Integer provinceID;

        @JsonProperty("ProvinceName")
        private String provinceName;

        @JsonProperty("Code")
        private String code;
    }
}
