package com.nitrotech.api.domain.product.usecase;

import com.nitrotech.api.domain.product.dto.ProductData;
import com.nitrotech.api.domain.product.repository.ProductRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class GetRelatedProductsUseCase {

    private final ProductRepository productRepository;

    public List<ProductData> execute(Long productId, int limit) {
        return productRepository.findRelated(productId, limit);
    }
}
