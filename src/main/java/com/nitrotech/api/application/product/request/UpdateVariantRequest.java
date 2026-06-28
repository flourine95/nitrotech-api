package com.nitrotech.api.application.product.request;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Positive;
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
        Boolean active,
        Long imageId,
        @Positive(message = "Weight must be greater than 0")
        Integer weightGrams,
        @DecimalMin(value = "0.0", inclusive = false, message = "Length must be greater than 0")
        BigDecimal lengthCm,
        @DecimalMin(value = "0.0", inclusive = false, message = "Width must be greater than 0")
        BigDecimal widthCm,
        @DecimalMin(value = "0.0", inclusive = false, message = "Height must be greater than 0")
        BigDecimal heightCm
) {}
