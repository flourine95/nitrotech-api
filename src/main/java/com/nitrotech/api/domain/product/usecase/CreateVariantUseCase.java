package com.nitrotech.api.domain.product.usecase;

import com.nitrotech.api.domain.product.dto.CreateVariantCommand;
import com.nitrotech.api.domain.product.dto.ProductVariantData;
import com.nitrotech.api.domain.product.repository.ProductRepository;
import com.nitrotech.api.shared.exception.ConflictException;
import com.nitrotech.api.shared.exception.NotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class CreateVariantUseCase {

    private final ProductRepository productRepository;

    public ProductVariantData execute(Long productId, CreateVariantCommand command) {
        if (!productRepository.existsById(productId)) {
            throw new NotFoundException("PRODUCT_NOT_FOUND", "Product not found");
        }
        if (productRepository.existsBySku(command.sku())) {
            throw new ConflictException("VARIANT_SKU_EXISTS", "SKU already exists");
        }
        return productRepository.createVariant(productId, command);
    }
}
