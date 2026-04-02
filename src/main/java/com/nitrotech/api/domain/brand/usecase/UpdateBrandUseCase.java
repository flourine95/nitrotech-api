package com.nitrotech.api.domain.brand.usecase;

import com.nitrotech.api.domain.brand.dto.BrandData;
import com.nitrotech.api.domain.brand.dto.UpdateBrandCommand;
import com.nitrotech.api.domain.brand.repository.BrandRepository;
import com.nitrotech.api.shared.exception.ConflictException;
import com.nitrotech.api.shared.exception.NotFoundException;
import org.springframework.stereotype.Service;

@Service
public class UpdateBrandUseCase {

    private final BrandRepository brandRepository;

    public UpdateBrandUseCase(BrandRepository brandRepository) {
        this.brandRepository = brandRepository;
    }

    public BrandData execute(UpdateBrandCommand command) {
        if (!brandRepository.existsById(command.id())) {
            throw new NotFoundException("BRAND_NOT_FOUND", "Brand not found");
        }
        if (command.slug() != null && brandRepository.existsBySlugAndIdNot(command.slug(), command.id())) {
            throw new ConflictException("BRAND_SLUG_EXISTS", "Slug already exists");
        }
        return brandRepository.update(command);
    }
}
