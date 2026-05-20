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

    /**
     * Get brand by ID or slug (auto-detect)
     * @param idOrSlug numeric ID or string slug
     */
    public BrandData execute(String idOrSlug) {
        // Try to parse as Long (ID)
        try {
            Long id = Long.parseLong(idOrSlug);
            return brandRepository.findById(id)
                    .orElseThrow(() -> new NotFoundException("BRAND_NOT_FOUND", "Brand not found"));
        } catch (NumberFormatException e) {
            // Not a number, treat as slug
            return brandRepository.findBySlug(idOrSlug)
                    .orElseThrow(() -> new NotFoundException("BRAND_NOT_FOUND", "Brand not found"));
        }
    }

    /**
     * Get brand by ID (deprecated, use execute(String) instead)
     */
    @Deprecated
    public BrandData execute(Long id) {
        return brandRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("BRAND_NOT_FOUND", "Brand not found"));
    }
}
