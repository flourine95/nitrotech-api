package com.nitrotech.api.domain.product.usecase;

import com.nitrotech.api.domain.product.repository.ProductRepository;
import com.nitrotech.api.shared.exception.NotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class DeleteProductUseCase {

    private final ProductRepository productRepository;

    public void execute(Long id) {
        if (!productRepository.existsById(id)) {
            throw new NotFoundException("PRODUCT_NOT_FOUND", "Product not found");
        }
        productRepository.softDelete(id);
    }
}
