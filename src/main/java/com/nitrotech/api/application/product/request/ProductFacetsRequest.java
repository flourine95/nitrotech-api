package com.nitrotech.api.application.product.request;

import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.util.List;

@Getter
@Setter
public class ProductFacetsRequest {

    @Size(max = 100, message = "Search query must not exceed 100 characters")
    private String search;

    private String category;

    private List<String> brand;

    private BigDecimal minPrice;

    private BigDecimal maxPrice;

    private Boolean active;

    private String badge;
}
