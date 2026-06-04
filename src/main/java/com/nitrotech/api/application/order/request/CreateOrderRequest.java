package com.nitrotech.api.application.order.request;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

public record CreateOrderRequest(
        Long addressId,

        @Valid
        ShippingAddressRequest shippingAddress,

        @Pattern(regexp = "^(cod|vnpay|momo)$", message = "Payment method must be cod, vnpay or momo")
        String paymentMethod,

        String promotionCode,
        String note
) {
    public record ShippingAddressRequest(
            @NotBlank(message = "Receiver name is required")
            String name,

            @NotBlank(message = "Phone number is required")
            String phone,

            @NotBlank(message = "Street address is required")
            String address,

            @NotBlank(message = "Ward is required")
            String ward,
            String wardCode,

            @NotBlank(message = "District is required")
            String district,
            String districtCode,

            @NotBlank(message = "City is required")
            String city,
            String cityCode,

            String country
    ) {}
}
