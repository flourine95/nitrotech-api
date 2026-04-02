package com.nitrotech.api.infrastructure.persistence.repository;

import com.nitrotech.api.domain.promotion.dto.CreatePromotionCommand;
import com.nitrotech.api.domain.promotion.dto.PromotionData;
import com.nitrotech.api.domain.promotion.repository.PromotionRepository;
import com.nitrotech.api.infrastructure.persistence.entity.PromotionEntity;
import com.nitrotech.api.infrastructure.persistence.entity.PromotionUsageEntity;
import com.nitrotech.api.shared.exception.NotFoundException;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public class PromotionRepositoryImpl implements PromotionRepository {

    private final PromotionJpaRepository jpa;
    private final PromotionUsageJpaRepository usageJpa;

    public PromotionRepositoryImpl(PromotionJpaRepository jpa, PromotionUsageJpaRepository usageJpa) {
        this.jpa = jpa;
        this.usageJpa = usageJpa;
    }

    @Override
    public PromotionData create(CreatePromotionCommand command) {
        return toData(jpa.save(toEntity(new PromotionEntity(), command)));
    }

    @Override
    public PromotionData update(Long id, CreatePromotionCommand command) {
        PromotionEntity entity = jpa.findById(id)
                .orElseThrow(() -> new NotFoundException("PROMOTION_NOT_FOUND", "Promotion not found"));
        return toData(jpa.save(toEntity(entity, command)));
    }

    @Override
    public Optional<PromotionData> findById(Long id) {
        return jpa.findById(id).map(this::toData);
    }

    @Override
    public Optional<PromotionData> findActiveByCode(String code) {
        return jpa.findActiveByCode(code, LocalDateTime.now()).map(this::toData);
    }

    @Override
    public List<PromotionData> findAll(String status, int page, int size) {
        return jpa.findAllFiltered(status, PageRequest.of(page, size))
                .getContent().stream().map(this::toData).toList();
    }

    @Override
    public long countAll(String status) {
        return jpa.findAllFiltered(status, PageRequest.of(0, Integer.MAX_VALUE)).getTotalElements();
    }

    @Override
    public PromotionData updateStatus(Long id, String status) {
        PromotionEntity entity = jpa.findById(id)
                .orElseThrow(() -> new NotFoundException("PROMOTION_NOT_FOUND", "Promotion not found"));
        entity.setStatus(status);
        entity.setUpdatedAt(LocalDateTime.now());
        return toData(jpa.save(entity));
    }

    @Override
    public void delete(Long id) {
        jpa.deleteById(id);
    }

    @Override
    public boolean existsById(Long id) { return jpa.existsById(id); }

    @Override
    public boolean existsByCode(String code) { return jpa.existsByCode(code); }

    @Override
    public boolean existsByCodeAndIdNot(String code, Long id) { return jpa.existsByCodeAndIdNot(code, id); }

    @Override
    public int countUsageByPromotion(Long promotionId) {
        return usageJpa.countByPromotionId(promotionId);
    }

    @Override
    public int countUsageByPromotionAndUser(Long promotionId, Long userId) {
        return usageJpa.countByPromotionIdAndUserId(promotionId, userId);
    }

    @Override
    public void recordUsage(Long promotionId, Long userId, Long orderId, String code, BigDecimal discountAmount) {
        PromotionUsageEntity usage = new PromotionUsageEntity();
        usage.setPromotionId(promotionId);
        usage.setUserId(userId);
        usage.setOrderId(orderId);
        usage.setUsedCode(code);
        usage.setDiscountAmount(discountAmount);
        usageJpa.save(usage);
    }

    private PromotionEntity toEntity(PromotionEntity e, CreatePromotionCommand c) {
        e.setName(c.name());
        e.setDescription(c.description());
        e.setCode(c.code());
        e.setType(c.type());
        e.setDiscountValue(c.discountValue());
        e.setMinOrderAmount(c.minOrderAmount() != null ? c.minOrderAmount() : BigDecimal.ZERO);
        e.setMaxDiscountAmount(c.maxDiscountAmount());
        e.setStackable(c.stackable());
        e.setPriority(c.priority());
        e.setUsageLimit(c.usageLimit());
        e.setUsagePerUser(c.usagePerUser());
        e.setStartAt(c.startAt());
        e.setEndAt(c.endAt());
        e.setStatus(c.status() != null ? c.status() : "draft");
        e.setUpdatedAt(LocalDateTime.now());
        return e;
    }

    private PromotionData toData(PromotionEntity e) {
        int totalUsed = usageJpa.countByPromotionId(e.getId());
        return new PromotionData(e.getId(), e.getName(), e.getDescription(), e.getCode(),
                e.getType(), e.getDiscountValue(), e.getMinOrderAmount(), e.getMaxDiscountAmount(),
                e.isStackable(), e.getPriority(), e.getUsageLimit(), e.getUsagePerUser(),
                e.getStartAt(), e.getEndAt(), e.getStatus(), totalUsed,
                e.getCreatedAt(), e.getUpdatedAt());
    }
}
