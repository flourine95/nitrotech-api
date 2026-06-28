package com.nitrotech.api.domain.brand.usecase;

import com.nitrotech.api.domain.brand.exception.BrandHasProductsException;
import com.nitrotech.api.domain.brand.exception.BrandNotFoundException;
import com.nitrotech.api.domain.brand.repository.BrandRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class HardDeleteBrandUseCase {

    private final BrandRepository brandRepository;
    private final ProductBrandChecker productBrandChecker;

    public void execute(Long id) {
        brandRepository.findDeletedById(id)
                .orElseThrow(BrandNotFoundException::deletedForHardDelete);

        if (productBrandChecker.hasProducts(id)) {
            throw new BrandHasProductsException();
        }

        brandRepository.hardDelete(id);
    }
}
