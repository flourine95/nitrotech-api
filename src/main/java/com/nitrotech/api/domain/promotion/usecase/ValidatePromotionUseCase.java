package com.nitrotech.api.domain.promotion.usecase;

import com.nitrotech.api.domain.promotion.dto.ApplyPromotionResult;
import com.nitrotech.api.domain.promotion.dto.PromotionData;
import com.nitrotech.api.domain.promotion.exception.OrderAmountTooLowException;
import com.nitrotech.api.domain.promotion.exception.PromotionNotFoundException;
import com.nitrotech.api.domain.promotion.exception.PromotionUsageLimitReachedException;
import com.nitrotech.api.domain.promotion.exception.PromotionUserLimitReachedException;
import com.nitrotech.api.domain.promotion.repository.PromotionRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;

@Service
@RequiredArgsConstructor
public class ValidatePromotionUseCase {

    private final PromotionRepository promotionRepository;

    public ApplyPromotionResult execute(String code, Long userId, BigDecimal orderAmount) {
        PromotionData promotion = promotionRepository.findActiveByCode(code)
                .orElseThrow(() -> PromotionNotFoundException.activeCodeNotFound());

        if (orderAmount.compareTo(promotion.minOrderAmount()) < 0) {
            throw new OrderAmountTooLowException(promotion.minOrderAmount());
        }

        if (promotion.usageLimit() != null) {
            int totalUsed = promotionRepository.countUsageByPromotion(promotion.id());
            if (totalUsed >= promotion.usageLimit()) {
                throw new PromotionUsageLimitReachedException();
            }
        }

        int userUsed = promotionRepository.countUsageByPromotionAndUser(promotion.id(), userId);
        if (userUsed >= promotion.usagePerUser()) {
            throw new PromotionUserLimitReachedException();
        }

        BigDecimal discount = calculateDiscount(promotion, orderAmount);
        return new ApplyPromotionResult(promotion.id(), code, discount, promotion.name());
    }

    private BigDecimal calculateDiscount(PromotionData promotion, BigDecimal orderAmount) {
        BigDecimal discount = switch (promotion.type()) {
            case "percentage" -> orderAmount.multiply(promotion.discountValue())
                    .divide(BigDecimal.valueOf(100), 2, RoundingMode.HALF_UP);
            case "fixed" -> promotion.discountValue();
            case "freeship" -> BigDecimal.ZERO; // handled separately
            default -> BigDecimal.ZERO;
        };

        if (promotion.maxDiscountAmount() != null) {
            discount = discount.min(promotion.maxDiscountAmount());
        }
        return discount.min(orderAmount);
    }
}
