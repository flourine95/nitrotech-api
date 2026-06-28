package com.nitrotech.api.domain.order.exception;

import com.nitrotech.api.shared.exception.DomainException;

public class CartEmptyException extends DomainException {

    public CartEmptyException() {
        super("CART_EMPTY", "Cart is empty");
    }
}
