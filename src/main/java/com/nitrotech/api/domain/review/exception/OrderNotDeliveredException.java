package com.nitrotech.api.domain.review.exception;

import com.nitrotech.api.shared.exception.DomainException;

public class OrderNotDeliveredException extends DomainException {

    public OrderNotDeliveredException() {
        super("ORDER_NOT_DELIVERED", "You can only review after order is delivered");
    }
}
