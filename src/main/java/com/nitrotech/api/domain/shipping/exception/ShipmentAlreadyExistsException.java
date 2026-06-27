package com.nitrotech.api.domain.shipping.exception;

import com.nitrotech.api.shared.exception.DomainException;

public class ShipmentAlreadyExistsException extends DomainException {

    public ShipmentAlreadyExistsException(Long orderId) {
        super("SHIPMENT_ALREADY_EXISTS", "Shipment already exists for order " + orderId);
    }
}
