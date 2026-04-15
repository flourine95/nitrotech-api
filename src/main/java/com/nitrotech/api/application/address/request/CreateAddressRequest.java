package com.nitrotech.api.application.address.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record CreateAddressRequest(
        @NotBlank(message = "Receiver name is required")
        @Size(max = 255)
        String receiver,

        @NotBlank(message = "Phone is required")
        @Pattern(regexp = "^[0-9]{9,11}$", message = "Phone must be 9-11 digits")
        String phone,

        @NotBlank(message = "Province is required")
        String province,

        @NotBlank(message = "Province code is required")
        String provinceCode,

        @NotBlank(message = "District is required")
        String district,

        @NotBlank(message = "District code is required")
        String districtCode,

        @NotBlank(message = "Ward is required")
        String ward,

        @NotBlank(message = "Ward code is required")
        String wardCode,

        @NotBlank(message = "Street is required")
        @Size(max = 255)
        String street,

        boolean defaultAddress
) {}
