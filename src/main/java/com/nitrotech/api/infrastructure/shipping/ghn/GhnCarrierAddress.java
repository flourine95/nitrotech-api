package com.nitrotech.api.infrastructure.shipping.ghn;

public record GhnCarrierAddress(
        Integer provinceId,
        Integer districtId,
        String wardCode
) {}
