package com.nitrotech.api.domain.shipping.provider;

import com.nitrotech.api.domain.order.dto.OrderData;
import com.nitrotech.api.domain.shipping.dto.ShippingFeeQuote;
import com.nitrotech.api.domain.shipping.dto.ShippingFeeQuoteRequest;
import com.nitrotech.api.domain.shipping.dto.ShippingResult;
import com.nitrotech.api.shared.exception.ShippingException;

public interface ShippingProvider {
    String getProviderName();

    ShippingResult createShipment(OrderData order);

    default ShippingFeeQuote quoteFee(ShippingFeeQuoteRequest request) {
        throw new ShippingException("SHIPPING_FEE_UNSUPPORTED",
                "Shipping fee quote is not supported for " + getProviderName());
    }
}
