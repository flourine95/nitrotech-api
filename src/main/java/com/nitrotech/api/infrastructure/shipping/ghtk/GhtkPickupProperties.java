package com.nitrotech.api.infrastructure.shipping.ghtk;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "ghtk.pickup")
public record GhtkPickupProperties(
        String name,
        String tel,
        String addressId,
        String address,
        String province,
        String district,
        String ward
) {}
