package com.nitrotech.api.shared.exception;

public class NotFoundException extends DomainException {

    public NotFoundException(String code, String message) {
        super(code, message);
    }
}
