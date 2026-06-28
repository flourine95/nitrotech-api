package com.nitrotech.api.domain.order.exception;

import com.nitrotech.api.shared.exception.DomainException;

public class PaymentMethodUnsupportedException extends DomainException {

    public PaymentMethodUnsupportedException(String paymentMethod) {
        super("PAYMENT_METHOD_UNSUPPORTED", "Payment method is not supported yet: " + paymentMethod);
    }
}
