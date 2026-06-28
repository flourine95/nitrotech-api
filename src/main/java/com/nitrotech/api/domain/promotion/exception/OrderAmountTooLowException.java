package com.nitrotech.api.domain.promotion.exception;

import com.nitrotech.api.shared.exception.DomainException;

import java.math.BigDecimal;

public class OrderAmountTooLowException extends DomainException {

    public OrderAmountTooLowException(BigDecimal minOrderAmount) {
        super("ORDER_AMOUNT_TOO_LOW", "Minimum order amount is " + minOrderAmount);
    }
}
