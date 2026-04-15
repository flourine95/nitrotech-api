package com.nitrotech.api.infrastructure.persistence.repository;

import com.nitrotech.api.infrastructure.persistence.entity.PromotionUsageEntity;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PromotionUsageJpaRepository extends JpaRepository<PromotionUsageEntity, Long> {
    int countByPromotionId(Long promotionId);
    int countByPromotionIdAndUserId(Long promotionId, Long userId);
}
