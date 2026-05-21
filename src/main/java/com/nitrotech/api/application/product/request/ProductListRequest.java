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
    private List<Long> categoryIds;
    private List<Long> brandIds;
    
    @PositiveOrZero(message = "Min price must be >= 0")
    private BigDecimal minPrice;
    
    @PositiveOrZero(message = "Max price must be >= 0")
    private BigDecimal maxPrice;
    
    @AssertTrue(message = "minPrice must be <= maxPrice")
    public boolean isPriceRangeValid() {
        if (minPrice == null || maxPrice == null) return true;
        return minPrice.compareTo(maxPrice) <= 0;
    }
    
    public ProductFilter toFilter() {
        return new ProductFilter(search, active, deleted,
                categoryIds, brandIds, minPrice, maxPrice);
    }
}
