package com.nitrotech.api.domain.product.usecase;

import com.nitrotech.api.domain.product.repository.ProductRepository;
import com.nitrotech.api.shared.exception.ConflictException;
import com.nitrotech.api.shared.exception.NotFoundException;
import org.springframework.stereotype.Service;

@Service
public class RestoreProductUseCase {

    private final ProductRepository productRepository;

    public RestoreProductUseCase(ProductRepository productRepository) {
        this.productRepository = productRepository;
    }

    public void execute(Long id) {
        var product = productRepository.findDeletedById(id)
                .orElseThrow(() -> new NotFoundException("PRODUCT_NOT_FOUND", "Deleted product not found"));

        if (productRepository.existsBySlugAndIdNot(product.slug(), id)) {
            throw new ConflictException("PRODUCT_SLUG_CONFLICT",
                    "Cannot restore: slug '" + product.slug() + "' is already used by another active product");
        }

        productRepository.restore(id);
    }
}
