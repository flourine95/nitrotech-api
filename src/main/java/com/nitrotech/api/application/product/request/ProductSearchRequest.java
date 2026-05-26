package com.nitrotech.api.application.product.request;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.util.List;

@Data
public class ProductSearchRequest {

    @Size(max = 100, message = "Search query must not exceed 100 characters")
    private String search;

    private String category;

    private String brand;

    private List<Long> excludeId;

    @Min(value = 1, message = "Limit must be at least 1")
    @Max(value = 100, message = "Limit must not exceed 100")
    private Integer limit = 20;
}
