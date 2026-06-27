package com.nitrotech.api.domain.brand.usecase;

import com.nitrotech.api.domain.brand.dto.BrandData;
import com.nitrotech.api.domain.brand.exception.BrandNotFoundException;
import com.nitrotech.api.domain.brand.repository.BrandRepository;
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
                    .orElseThrow(() -> BrandNotFoundException.withId(id));
        } catch (NumberFormatException e) {
            return brandRepository.findVisibleBySlug(idOrSlug)
                    .orElseThrow(() -> BrandNotFoundException.withSlug(idOrSlug));
        }
    }
}
