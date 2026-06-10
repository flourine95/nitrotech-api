package com.nitrotech.api.shared.exception;

public class ShippingException extends DomainException {
    public ShippingException(String code, String message) {
        super(code, message);
    }
}
