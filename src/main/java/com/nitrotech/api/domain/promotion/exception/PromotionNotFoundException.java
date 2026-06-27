package com.nitrotech.api.domain.promotion.exception;

import com.nitrotech.api.shared.exception.NotFoundException;

public class PromotionNotFoundException extends NotFoundException {

    public PromotionNotFoundException() {
        super("PROMOTION_NOT_FOUND", "Promotion not found");
    }

    private PromotionNotFoundException(String message) {
        super("PROMOTION_NOT_FOUND", message);
    }

    public static PromotionNotFoundException activeCodeNotFound() {
        return new PromotionNotFoundException("Promotion code not found or expired");
    }
}
