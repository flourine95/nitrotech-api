package com.nitrotech.api.domain.shipping.exception;

import com.nitrotech.api.shared.exception.NotFoundException;

public class ShipmentNotFoundException extends NotFoundException {

    public ShipmentNotFoundException() {
        super("SHIPMENT_NOT_FOUND", "Shipment not found");
    }

    private ShipmentNotFoundException(String message) {
        super("SHIPMENT_NOT_FOUND", message);
    }

    public static ShipmentNotFoundException withTrackingCode(String trackingCode) {
        return new ShipmentNotFoundException("Shipment with tracking code " + trackingCode + " not found");
    }

    public static ShipmentNotFoundException withId(Long id) {
        return new ShipmentNotFoundException("Shipment with ID " + id + " not found");
    }
}
