package com.nitrotech.api.domain.product.usecase;

import com.nitrotech.api.domain.product.exception.ProductNotFoundException;
import com.nitrotech.api.domain.product.exception.ProductSlugConflictException;

import com.nitrotech.api.domain.product.repository.ProductRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class RestoreProductUseCase {

    private final ProductRepository productRepository;

    public void execute(Long id) {
        var product = productRepository.findDeletedById(id)
                .orElseThrow(() -> ProductNotFoundException.deleted());

        if (productRepository.existsNotDeletedBySlugAndIdNot(product.slug(), id)) {
            throw new ProductSlugConflictException(product.slug());
        }

        productRepository.restore(id);
    }
}
