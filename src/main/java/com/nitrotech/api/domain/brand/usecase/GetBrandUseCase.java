package com.nitrotech.api.domain.brand.usecase;

import com.nitrotech.api.domain.brand.dto.BrandData;
import com.nitrotech.api.domain.brand.repository.BrandRepository;
import com.nitrotech.api.shared.exception.NotFoundException;
import org.springframework.stereotype.Service;

@Service
public class GetBrandUseCase {

    private final BrandRepository brandRepository;

    public GetBrandUseCase(BrandRepository brandRepository) {
        this.brandRepository = brandRepository;
    }

    public BrandData execute(Long id) {
        return brandRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("BRAND_NOT_FOUND", "Brand not found"));
    }
}
