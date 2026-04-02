package com.nitrotech.api.domain.brand.usecase;

import com.nitrotech.api.domain.brand.dto.BrandData;
import com.nitrotech.api.domain.brand.repository.BrandRepository;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class GetBrandsUseCase {

    private final BrandRepository brandRepository;

    public GetBrandsUseCase(BrandRepository brandRepository) {
        this.brandRepository = brandRepository;
    }

    public List<BrandData> execute(Boolean active) {
        return brandRepository.findAll(active);
    }
}
