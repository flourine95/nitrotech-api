package com.nitrotech.api.domain.order.exception;

import com.nitrotech.api.shared.exception.NotFoundException;

public class OrderNotFoundException extends NotFoundException {

    public OrderNotFoundException() {
        super("ORDER_NOT_FOUND", "Order not found");
    }

    private OrderNotFoundException(String message) {
        super("ORDER_NOT_FOUND", message);
    }

    public static OrderNotFoundException withId(Long id) {
        return new OrderNotFoundException("Order with ID " + id + " not found");
    }
}
