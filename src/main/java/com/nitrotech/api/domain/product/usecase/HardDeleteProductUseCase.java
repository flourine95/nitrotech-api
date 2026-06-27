package com.nitrotech.api.domain.product.usecase;

import com.nitrotech.api.domain.product.exception.ProductNotFoundException;
import com.nitrotech.api.domain.product.repository.ProductRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class HardDeleteProductUseCase {

    private final ProductRepository productRepository;

    public void execute(Long id) {
        productRepository.findDeletedById(id)
                .orElseThrow(ProductNotFoundException::deletedForHardDelete);

        productRepository.hardDelete(id);
    }
}
