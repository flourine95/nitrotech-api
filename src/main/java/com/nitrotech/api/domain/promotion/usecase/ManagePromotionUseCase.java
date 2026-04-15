package com.nitrotech.api.domain.promotion.usecase;

import com.nitrotech.api.domain.promotion.dto.CreatePromotionCommand;
import com.nitrotech.api.domain.promotion.dto.PromotionData;
import com.nitrotech.api.domain.promotion.repository.PromotionRepository;
import com.nitrotech.api.shared.exception.ConflictException;
import com.nitrotech.api.shared.exception.DomainException;
import com.nitrotech.api.shared.exception.NotFoundException;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class ManagePromotionUseCase {

    private final PromotionRepository promotionRepository;

    public ManagePromotionUseCase(PromotionRepository promotionRepository) {
        this.promotionRepository = promotionRepository;
    }

    public PromotionData create(CreatePromotionCommand command) {
        if (command.code() != null && promotionRepository.existsByCode(command.code())) {
            throw new ConflictException("PROMOTION_CODE_EXISTS", "Promotion code already exists");
        }
        if (command.startAt().isAfter(command.endAt())) {
            throw new DomainException("INVALID_DATE_RANGE", "Start date must be before end date") {};
        }
        return promotionRepository.create(command);
    }

    public PromotionData update(Long id, CreatePromotionCommand command) {
        if (!promotionRepository.existsById(id)) {
            throw new NotFoundException("PROMOTION_NOT_FOUND", "Promotion not found");
        }
        if (command.code() != null && promotionRepository.existsByCodeAndIdNot(command.code(), id)) {
            throw new ConflictException("PROMOTION_CODE_EXISTS", "Promotion code already exists");
        }
        return promotionRepository.update(id, command);
    }

    public PromotionData updateStatus(Long id, String status) {
        if (!promotionRepository.existsById(id)) {
            throw new NotFoundException("PROMOTION_NOT_FOUND", "Promotion not found");
        }
        return promotionRepository.updateStatus(id, status);
    }

    public List<PromotionData> findAll(String status, int page, int size) {
        return promotionRepository.findAll(status, page, size);
    }

    public long countAll(String status) {
        return promotionRepository.countAll(status);
    }

    public PromotionData findById(Long id) {
        return promotionRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("PROMOTION_NOT_FOUND", "Promotion not found"));
    }

    public void delete(Long id) {
        if (!promotionRepository.existsById(id)) {
            throw new NotFoundException("PROMOTION_NOT_FOUND", "Promotion not found");
        }
        promotionRepository.delete(id);
    }
}
