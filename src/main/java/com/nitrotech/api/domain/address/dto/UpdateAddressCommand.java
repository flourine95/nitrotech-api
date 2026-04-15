package com.nitrotech.api.domain.address.dto;

public record UpdateAddressCommand(
        Long id,
        Long userId,
        String receiver,
        String phone,
        String province,
        String provinceCode,
        String district,
        String districtCode,
        String ward,
        String wardCode,
        String street,
        Boolean defaultAddress
) {}
