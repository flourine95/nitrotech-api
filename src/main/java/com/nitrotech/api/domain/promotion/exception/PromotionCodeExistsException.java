package com.nitrotech.api.domain.promotion.exception;

import com.nitrotech.api.shared.exception.ConflictException;

public class PromotionCodeExistsException extends ConflictException {

    public PromotionCodeExistsException() {
        super("PROMOTION_CODE_EXISTS", "Promotion code already exists");
    }
}
