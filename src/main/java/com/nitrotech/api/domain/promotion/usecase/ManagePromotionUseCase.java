package com.nitrotech.api.domain.promotion.usecase;

import com.nitrotech.api.domain.promotion.dto.CreatePromotionCommand;
import com.nitrotech.api.domain.promotion.dto.PromotionData;
import com.nitrotech.api.domain.promotion.exception.PromotionCodeExistsException;
import com.nitrotech.api.domain.promotion.exception.PromotionNotFoundException;
import com.nitrotech.api.domain.promotion.repository.PromotionRepository;
import com.nitrotech.api.domain.shared.exception.InvalidDateRangeException;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class ManagePromotionUseCase {

    private final PromotionRepository promotionRepository;

    public PromotionData create(CreatePromotionCommand command) {
        if (command.code() != null && promotionRepository.existsByCode(command.code())) {
            throw new PromotionCodeExistsException();
        }
        if (command.startAt().isAfter(command.endAt())) {
            throw new InvalidDateRangeException();
        }
        return promotionRepository.create(command);
    }

    public PromotionData update(Long id, CreatePromotionCommand command) {
        if (!promotionRepository.existsById(id)) {
            throw new PromotionNotFoundException();
        }
        if (command.code() != null && promotionRepository.existsByCodeAndIdNot(command.code(), id)) {
            throw new PromotionCodeExistsException();
        }
        return promotionRepository.update(id, command);
    }

    public PromotionData updateStatus(Long id, String status) {
        if (!promotionRepository.existsById(id)) {
            throw new PromotionNotFoundException();
        }
        return promotionRepository.updateStatus(id, status);
    }

    public Page<PromotionData> findAll(String status, int page, int size) {
        return promotionRepository.findAll(status, PageRequest.of(page, size));
    }

    public PromotionData findById(Long id) {
        return promotionRepository.findById(id)
                .orElseThrow(() -> new PromotionNotFoundException());
    }

    public void delete(Long id) {
        if (!promotionRepository.existsById(id)) {
            throw new PromotionNotFoundException();
        }
        promotionRepository.delete(id);
    }
}
