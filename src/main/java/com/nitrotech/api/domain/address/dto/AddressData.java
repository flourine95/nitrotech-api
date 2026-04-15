package com.nitrotech.api.domain.address.dto;

import java.time.LocalDateTime;

public record AddressData(
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
        boolean defaultAddress,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {}
