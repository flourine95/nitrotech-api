package com.nitrotech.api.domain.shipping.dto;

import com.nitrotech.api.domain.order.dto.OrderItemData;
import com.nitrotech.api.domain.order.dto.ShippingAddressSnapshot;

import java.math.BigDecimal;
import java.util.List;

public record ShippingFeeQuoteRequest(
        ShippingAddressSnapshot shippingAddress,
        List<OrderItemData> items,
        BigDecimal orderValue
) {}
