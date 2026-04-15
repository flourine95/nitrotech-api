package com.nitrotech.api.domain.brand.usecase;

import com.nitrotech.api.domain.brand.repository.BrandRepository;
import com.nitrotech.api.shared.exception.NotFoundException;
import org.springframework.stereotype.Service;

@Service
public class DeleteBrandUseCase {

    private final BrandRepository brandRepository;

    public DeleteBrandUseCase(BrandRepository brandRepository) {
        this.brandRepository = brandRepository;
    }

    public void execute(Long id) {
        if (!brandRepository.existsById(id)) {
            throw new NotFoundException("BRAND_NOT_FOUND", "Brand not found");
        }
        brandRepository.softDelete(id);
    }
}
