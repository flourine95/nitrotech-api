package com.nitrotech.api.domain.inventory.exception;

import com.nitrotech.api.shared.exception.DomainException;

public class InsufficientStockException extends DomainException {

    public InsufficientStockException(int available) {
        super("INSUFFICIENT_STOCK", "Insufficient stock. Available: " + available);
    }

    public InsufficientStockException(String itemName, int available) {
        super("INSUFFICIENT_STOCK", "Insufficient stock for " + itemName + ". Available: " + available);
    }
}
