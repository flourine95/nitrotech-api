package com.nitrotech.api.application.product.request;

import com.nitrotech.api.domain.product.dto.ProductFilter;
import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.math.BigDecimal;
import java.util.List;

@Data
public class ProductListRequest {

    @Size(max = 100, message = "Search query must not exceed 100 characters")
    private String search;

    private Boolean active;

    private Boolean deleted;

    private String category;

    private List<String> brand;

    @PositiveOrZero(message = "Min price must be greater than or equal to 0")
    private BigDecimal minPrice;

    @PositiveOrZero(message = "Max price must be greater than or equal to 0")
    private BigDecimal maxPrice;

    @AssertTrue(message = "Min price must be less than or equal to max price")
    public boolean isPriceRangeValid() {
        if (minPrice == null || maxPrice == null) return true;
        return minPrice.compareTo(maxPrice) <= 0;
    }

    public ProductFilter toFilter() {
        return new ProductFilter(
                search,
                active,
                deleted,
                category,
                brand,
                minPrice,
                maxPrice
        );
    }
}
