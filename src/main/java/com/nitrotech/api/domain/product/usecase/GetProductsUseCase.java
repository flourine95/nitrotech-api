package com.nitrotech.api.domain.product.usecase;

import com.nitrotech.api.domain.product.dto.ProductData;
import com.nitrotech.api.domain.product.dto.ProductFilter;
import com.nitrotech.api.domain.product.repository.ProductRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class GetProductsUseCase {

    private final ProductRepository productRepository;

    public Page<ProductData> execute(ProductFilter filter, Pageable pageable) {
        boolean isPriceSort = pageable.getSort().stream()
                .anyMatch(order -> "price".equals(order.getProperty()));
        
        if (isPriceSort) {
            return productRepository.findAllSortedByPrice(filter, pageable);
        }
        
        return productRepository.findAll(filter, pageable);
    }
}
