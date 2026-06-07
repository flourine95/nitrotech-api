package com.nitrotech.api.application.address.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record UpdateAddressRequest(
    @NotBlank(message = "Receiver name is required")
    @Size(max = 255, message = "Receiver name must not exceed 255 characters")
    String receiver,

    @NotBlank(message = "Phone number is required")
    @Pattern(regexp = "^0[0-9]{9}$", message = "Phone number must be 10 digits starting with 0")
    String phone,

    @NotBlank(message = "Province is required")
    @Size(max = 100, message = "Province must not exceed 100 characters")
    String province,

    @NotBlank(message = "Province code is required")
    @Size(max = 20, message = "Province code must not exceed 20 characters")
    String provinceCode,

    @Size(max = 100, message = "District must not exceed 100 characters")
    String district,

    @Size(max = 20, message = "District code must not exceed 20 characters")
    String districtCode,

    @NotBlank(message = "Ward is required")
    @Size(max = 100, message = "Ward must not exceed 100 characters")
    String ward,

    @NotBlank(message = "Ward code is required")
    @Size(max = 20, message = "Ward code must not exceed 20 characters")
    String wardCode,

    @NotBlank(message = "Street address is required")
    @Size(max = 255, message = "Street address must not exceed 255 characters")
    String street,

    Boolean defaultAddress
) {
    public UpdateAddressRequest {
        if (defaultAddress == null) {
            defaultAddress = false;
        }
    }
}
