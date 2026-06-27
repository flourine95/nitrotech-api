package com.nitrotech.api.domain.promotion.repository;

import com.nitrotech.api.domain.promotion.dto.CreatePromotionCommand;
import com.nitrotech.api.domain.promotion.dto.PromotionData;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.math.BigDecimal;
import java.util.Optional;

public interface PromotionRepository {
    PromotionData create(CreatePromotionCommand command);

    PromotionData update(Long id, CreatePromotionCommand command);

    Optional<PromotionData> findById(Long id);

    Optional<PromotionData> findActiveByCode(String code);

    Page<PromotionData> findAll(String status, Pageable pageable);

    PromotionData updateStatus(Long id, String status);

    void delete(Long id);

    boolean existsById(Long id);

    boolean existsByCode(String code);

    boolean existsByCodeAndIdNot(String code, Long id);

    int countUsageByPromotion(Long promotionId);

    int countUsageByPromotionAndUser(Long promotionId, Long userId);

    void recordUsage(Long promotionId, Long userId, Long orderId, String code, BigDecimal discountAmount);
}
