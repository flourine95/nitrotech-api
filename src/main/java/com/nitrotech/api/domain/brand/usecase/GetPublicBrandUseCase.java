package com.nitrotech.api.domain.brand.usecase;

import com.nitrotech.api.domain.brand.dto.BrandData;
import com.nitrotech.api.domain.brand.repository.BrandRepository;
import com.nitrotech.api.shared.exception.NotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class GetPublicBrandUseCase {

    private final BrandRepository brandRepository;

    public BrandData execute(String idOrSlug) {
        try {
            Long id = Long.parseLong(idOrSlug);
            return brandRepository.findVisibleById(id)
                    .orElseThrow(() -> new NotFoundException("BRAND_NOT_FOUND",
                            "Brand with ID " + id + " not found"));
        } catch (NumberFormatException e) {
            return brandRepository.findVisibleBySlug(idOrSlug)
                    .orElseThrow(() -> new NotFoundException("BRAND_NOT_FOUND",
                            "Brand with slug '" + idOrSlug + "' not found"));
        }
    }
}
