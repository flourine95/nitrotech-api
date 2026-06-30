package com.nitrotech.api.infrastructure.payment.vnpay;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "vnpay")
public record VnpayProperties(
        String tmnCode,
        String hashSecret,
        String payUrl,
        String returnUrl,
        String ipnUrl,
        String locale,
        String orderType,
        Integer expireMinutes
) {}
