package com.nitrotech.api.domain.product.usecase;

import com.nitrotech.api.domain.product.dto.ProductFacets;
import com.nitrotech.api.domain.product.dto.ProductFilter;
import com.nitrotech.api.domain.product.repository.ProductRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class GetProductFacetsUseCase {

    private final ProductRepository productRepository;

    public ProductFacets execute(ProductFilter filter) {
        return productRepository.getFacets(filter);
    }
}
