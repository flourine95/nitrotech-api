package com.nitrotech.api.domain.promotion.exception;

import com.nitrotech.api.shared.exception.DomainException;

public class PromotionUsageLimitReachedException extends DomainException {

    public PromotionUsageLimitReachedException() {
        super("PROMOTION_USAGE_LIMIT_REACHED", "Promotion usage limit reached");
    }
}
