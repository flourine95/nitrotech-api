package com.nitrotech.api.domain.brand.usecase;

import com.nitrotech.api.domain.brand.exception.BrandNotFoundException;
import com.nitrotech.api.domain.brand.exception.BrandSlugConflictException;

import com.nitrotech.api.domain.brand.repository.BrandRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class RestoreBrandUseCase {

    private final BrandRepository brandRepository;

    public void execute(Long id) {
        var brand = brandRepository.findDeletedById(id)
                .orElseThrow(() -> BrandNotFoundException.deleted());

        if (brandRepository.existsNotDeletedBySlugAndIdNot(brand.slug(), id)) {
            throw new BrandSlugConflictException(brand.slug());
        }

        brandRepository.restore(id);
    }
}
