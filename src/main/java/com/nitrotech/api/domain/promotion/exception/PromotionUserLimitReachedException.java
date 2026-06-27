package com.nitrotech.api.domain.promotion.exception;

import com.nitrotech.api.shared.exception.DomainException;

public class PromotionUserLimitReachedException extends DomainException {

    public PromotionUserLimitReachedException() {
        super("PROMOTION_USER_LIMIT_REACHED", "You have already used this promotion");
    }
}
