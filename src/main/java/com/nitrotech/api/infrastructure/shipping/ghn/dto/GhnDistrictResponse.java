package com.nitrotech.api.infrastructure.shipping.ghn.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class GhnDistrictResponse {
    private Integer code;
    private String message;
    private List<DistrictData> data;

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class DistrictData {
        @JsonProperty("DistrictID")
        private Integer districtID;

        @JsonProperty("ProvinceID")
        private Integer provinceID;

        @JsonProperty("DistrictName")
        private String districtName;

        @JsonProperty("Code")
        private String code;
    }
}
