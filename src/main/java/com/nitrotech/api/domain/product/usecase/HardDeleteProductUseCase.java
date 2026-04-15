package com.nitrotech.api.domain.product.usecase;

import com.nitrotech.api.domain.product.repository.ProductRepository;
import com.nitrotech.api.shared.exception.NotFoundException;
import org.springframework.stereotype.Service;

@Service
public class HardDeleteProductUseCase {

    private final ProductRepository productRepository;

    public HardDeleteProductUseCase(ProductRepository productRepository) {
        this.productRepository = productRepository;
    }

    public void execute(Long id) {
        productRepository.findDeletedById(id)
                .orElseThrow(() -> new NotFoundException("PRODUCT_NOT_FOUND",
                        "Deleted product not found. Soft delete first before permanent delete."));

        productRepository.hardDelete(id);
    }
}
