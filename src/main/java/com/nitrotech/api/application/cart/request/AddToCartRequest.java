package com.nitrotech.api.application.cart.request;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

public record AddToCartRequest(
        @NotNull(message = "Variant is required")
        Long variantId,

        @Min(value = 1, message = "Quantity must be at least 1")
        int quantity
) {}
