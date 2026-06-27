package com.nitrotech.api.domain.order.exception;

import com.nitrotech.api.shared.exception.DomainException;

public class OrderCannotCancelException extends DomainException {

    public OrderCannotCancelException(String status) {
        super("ORDER_CANNOT_CANCEL", "Order cannot be cancelled in status: " + status);
    }
}
