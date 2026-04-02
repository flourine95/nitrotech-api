package com.nitrotech.api.application.product.request;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;
import java.util.Map;

public record UpdateVariantRequest(
        @Size(max = 100, message = "SKU must be at most 100 characters")
        String sku,

        @Size(max = 255, message = "Name must be at most 255 characters")
        String name,

        @DecimalMin(value = "0.0", inclusive = false, message = "Price must be greater than 0")
        BigDecimal price,

        Map<String, Object> attributes,
        Boolean active
) {}
