package com.nitrotech.api.domain.brand.usecase;

import com.nitrotech.api.domain.brand.repository.BrandRepository;
import com.nitrotech.api.shared.exception.ConflictException;
import com.nitrotech.api.shared.exception.NotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class HardDeleteBrandUseCase {

    private final BrandRepository brandRepository;
    private final ProductBrandChecker productBrandChecker;

    public void execute(Long id) {
        brandRepository.findDeletedById(id)
                .orElseThrow(() -> new NotFoundException("BRAND_NOT_FOUND",
                        "Deleted brand not found. Soft delete first before permanent delete."));

        if (productBrandChecker.hasProducts(id)) {
            throw new ConflictException("BRAND_HAS_PRODUCTS",
                    "Cannot permanently delete brand that has products.");
        }

        brandRepository.hardDelete(id);
    }
}
