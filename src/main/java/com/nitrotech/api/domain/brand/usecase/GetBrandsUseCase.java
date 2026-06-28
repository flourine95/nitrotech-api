package com.nitrotech.api.domain.brand.usecase;

import com.nitrotech.api.domain.brand.dto.BrandFilter;
import com.nitrotech.api.domain.brand.dto.BrandListResult;
import com.nitrotech.api.domain.brand.repository.BrandRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class GetBrandsUseCase {

    private final BrandRepository brandRepository;

    public BrandListResult execute(BrandFilter filter, Pageable pageable) {
        return new BrandListResult(
                brandRepository.findAll(filter, pageable),
                brandRepository.countFacets(filter.search())
        );
    }
}
