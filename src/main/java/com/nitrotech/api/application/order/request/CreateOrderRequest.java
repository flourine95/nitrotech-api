package com.nitrotech.api.application.order.request;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;

public record CreateOrderRequest(
        @NotNull(message = "Address is required")
        Long addressId,

        @Pattern(regexp = "^(cod|vnpay|momo)$", message = "Payment method must be cod, vnpay or momo")
        String paymentMethod,

        String promotionCode,
        String note
) {}
