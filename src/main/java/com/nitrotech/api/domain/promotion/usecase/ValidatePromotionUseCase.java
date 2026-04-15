package com.nitrotech.api.domain.promotion.usecase;

import com.nitrotech.api.domain.promotion.dto.ApplyPromotionResult;
import com.nitrotech.api.domain.promotion.dto.PromotionData;
import com.nitrotech.api.domain.promotion.repository.PromotionRepository;
import com.nitrotech.api.shared.exception.DomainException;
import com.nitrotech.api.shared.exception.NotFoundException;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;

@Service
public class ValidatePromotionUseCase {

    private final PromotionRepository promotionRepository;

    public ValidatePromotionUseCase(PromotionRepository promotionRepository) {
        this.promotionRepository = promotionRepository;
    }

    public ApplyPromotionResult execute(String code, Long userId, BigDecimal orderAmount) {
        PromotionData promotion = promotionRepository.findActiveByCode(code)
                .orElseThrow(() -> new NotFoundException("PROMOTION_NOT_FOUND", "Promotion code not found or expired"));

        if (orderAmount.compareTo(promotion.minOrderAmount()) < 0) {
            throw new DomainException("ORDER_AMOUNT_TOO_LOW",
                    "Minimum order amount is " + promotion.minOrderAmount()) {};
        }

        if (promotion.usageLimit() != null) {
            int totalUsed = promotionRepository.countUsageByPromotion(promotion.id());
            if (totalUsed >= promotion.usageLimit()) {
                throw new DomainException("PROMOTION_USAGE_LIMIT_REACHED", "Promotion usage limit reached") {};
            }
        }

        int userUsed = promotionRepository.countUsageByPromotionAndUser(promotion.id(), userId);
        if (userUsed >= promotion.usagePerUser()) {
            throw new DomainException("PROMOTION_USER_LIMIT_REACHED", "You have already used this promotion") {};
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
