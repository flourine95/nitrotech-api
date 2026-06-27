package com.nitrotech.api.domain.order.dto;

import com.nitrotech.api.domain.address.dto.AddressData;

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
) {

    public static ShippingAddressSnapshot from(AddressData address) {
        return new ShippingAddressSnapshot(
                address.receiver(), address.phone(),
                address.province(), address.provinceCode(),
                address.district(), address.districtCode(),
                address.ward(), address.wardCode(),
                address.street()
        );
    }
}
