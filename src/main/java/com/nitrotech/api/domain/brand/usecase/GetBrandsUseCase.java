package com.nitrotech.api.domain.brand.usecase;

import com.nitrotech.api.domain.brand.dto.BrandData;
import com.nitrotech.api.domain.brand.dto.BrandFilter;
import com.nitrotech.api.domain.brand.repository.BrandRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

@Service
public class GetBrandsUseCase {

    private final BrandRepository brandRepository;

    public GetBrandsUseCase(BrandRepository brandRepository) {
        this.brandRepository = brandRepository;
    }

    public Page<BrandData> execute(BrandFilter filter, Pageable pageable) {
        return brandRepository.findAll(filter, pageable);
    }
}
