package com.nitrotech.api.domain.cart.exception;

import com.nitrotech.api.shared.exception.NotFoundException;

public class CartItemNotFoundException extends NotFoundException {

    public CartItemNotFoundException() {
        super("CART_ITEM_NOT_FOUND", "Item not found in cart");
    }
}
