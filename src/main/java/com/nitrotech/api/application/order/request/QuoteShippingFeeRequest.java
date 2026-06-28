package com.nitrotech.api.application.order.request;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;

public record QuoteShippingFeeRequest(
        @NotNull(message = "Shipping address is required")
        @Valid
        CreateOrderRequest.ShippingAddressRequest shippingAddress
) {}
