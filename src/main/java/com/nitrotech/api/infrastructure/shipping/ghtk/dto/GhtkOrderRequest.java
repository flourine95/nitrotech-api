package com.nitrotech.api.infrastructure.shipping.ghtk.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class GhtkOrderRequest {
    private List<Product> products;
    private Order order;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Product {
        private String name;
        private Double weight; // in kg
        private Integer quantity;
        @JsonProperty("product_code")
        private String productCode;
        private BigDecimal price;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Order {
        private String id; // partner_order_id
        private String tel;
        private String name;
        private String address;
        private String province;
        private String district;
        private String ward;
        private String street;
        private String hamlet;
        @JsonProperty("is_freeship")
        private Integer isFreeship; // 1: shop pays, 0: buyer pays
        @JsonProperty("pick_money")
        private BigDecimal pickMoney; // COD amount
        private String note;
        private BigDecimal value; // declared package value for insurance
    }
}
