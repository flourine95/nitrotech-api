package com.nitrotech.api.domain.brand.usecase;

import com.nitrotech.api.domain.brand.dto.BrandData;
import com.nitrotech.api.domain.brand.dto.UpdateBrandCommand;
import com.nitrotech.api.domain.brand.repository.BrandRepository;
import com.nitrotech.api.shared.exception.ConflictException;
import com.nitrotech.api.shared.exception.NotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class UpdateBrandUseCase {

    private final BrandRepository brandRepository;

    @Transactional
    public BrandData execute(UpdateBrandCommand command) {
        if (!brandRepository.existsById(command.id())) {
            throw new NotFoundException("BRAND_NOT_FOUND", "Brand not found");
        }
        if (command.slug() != null && brandRepository.existsNotDeletedBySlugAndIdNot(command.slug(), command.id())) {
            throw new ConflictException("BRAND_SLUG_EXISTS", "Slug already exists");
        }
        return brandRepository.update(command);
    }
}
