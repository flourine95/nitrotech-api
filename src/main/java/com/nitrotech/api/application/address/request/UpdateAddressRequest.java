package com.nitrotech.api.application.address.request;

import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record UpdateAddressRequest(
        @Size(max = 255)
        String receiver,

        @Pattern(regexp = "^[0-9]{9,11}$", message = "Phone must be 9-11 digits")
        String phone,

        String province,
        String provinceCode,
        String district,
        String districtCode,
        String ward,
        String wardCode,

        @Size(max = 255)
        String street,

        Boolean defaultAddress
) {}
