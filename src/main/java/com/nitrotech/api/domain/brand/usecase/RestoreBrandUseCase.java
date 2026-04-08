package com.nitrotech.api.domain.brand.usecase;

import com.nitrotech.api.domain.brand.repository.BrandRepository;
import com.nitrotech.api.shared.exception.ConflictException;
import com.nitrotech.api.shared.exception.NotFoundException;
import org.springframework.stereotype.Service;

@Service
public class RestoreBrandUseCase {

    private final BrandRepository brandRepository;

    public RestoreBrandUseCase(BrandRepository brandRepository) {
        this.brandRepository = brandRepository;
    }

    public void execute(Long id) {
        var brand = brandRepository.findDeletedById(id)
                .orElseThrow(() -> new NotFoundException("BRAND_NOT_FOUND", "Deleted brand not found"));

        if (brandRepository.existsBySlugAndIdNot(brand.slug(), id)) {
            throw new ConflictException("BRAND_SLUG_CONFLICT",
                    "Cannot restore: slug '" + brand.slug() + "' is already used by another active brand");
        }

        brandRepository.restore(id);
    }
}
