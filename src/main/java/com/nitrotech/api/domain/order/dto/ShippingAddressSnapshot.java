package com.nitrotech.api.domain.order.dto;

public record ShippingAddressSnapshot(
        String receiver,
        String phone,
        String province,
        String provinceCode,
        String district,
        String districtCode,
        String ward,
        String wardCode,
        String street
) {}
