package com.nitrotech.api.domain.brand.usecase;

import com.nitrotech.api.domain.brand.dto.BrandData;
import com.nitrotech.api.domain.brand.dto.CreateBrandCommand;
import com.nitrotech.api.domain.brand.repository.BrandRepository;
import com.nitrotech.api.shared.exception.ConflictException;
import org.springframework.stereotype.Service;

@Service
public class CreateBrandUseCase {

    private final BrandRepository brandRepository;

    public CreateBrandUseCase(BrandRepository brandRepository) {
        this.brandRepository = brandRepository;
    }

    public BrandData execute(CreateBrandCommand command) {
        if (brandRepository.existsBySlug(command.slug())) {
            throw new ConflictException("BRAND_SLUG_EXISTS", "Slug already exists");
        }
        return brandRepository.create(command);
    }
}
