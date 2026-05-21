package com.nitrotech.api.domain.brand.usecase;

import com.nitrotech.api.domain.brand.dto.BrandData;
import com.nitrotech.api.domain.brand.repository.BrandRepository;
import com.nitrotech.api.shared.exception.NotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class GetBrandUseCase {

    private final BrandRepository brandRepository;

    public BrandData execute(String idOrSlug) {
        try {
            Long id = Long.parseLong(idOrSlug);
            return brandRepository.findById(id)
                    .orElseThrow(() -> new NotFoundException("BRAND_NOT_FOUND", "Brand not found"));
        } catch (NumberFormatException e) {
            return brandRepository.findBySlug(idOrSlug)
                    .orElseThrow(() -> new NotFoundException("BRAND_NOT_FOUND", "Brand not found"));
        }
    }

    @Deprecated
    public BrandData execute(Long id) {
        return brandRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("BRAND_NOT_FOUND", "Brand not found"));
    }
}
