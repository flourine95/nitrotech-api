package com.nitrotech.api.domain.shipping.provider;

import com.nitrotech.api.domain.order.dto.OrderData;
import com.nitrotech.api.domain.shipping.dto.ShippingResult;

public interface ShippingProvider {
    String getProviderName();
    ShippingResult createShipment(OrderData order);
}
