package com.nitrotech.api.domain.shipping.exception;

import com.nitrotech.api.shared.exception.DomainException;

public class InvalidShipmentOrderStatusException extends DomainException {

    public InvalidShipmentOrderStatusException(Long orderId, String status) {
        super("INVALID_ORDER_STATUS", "Cannot create shipment for order " + orderId + " in " + status + " status");
    }
}
