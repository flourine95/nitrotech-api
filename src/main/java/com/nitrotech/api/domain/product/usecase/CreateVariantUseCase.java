package com.nitrotech.api.domain.product.usecase;

import com.nitrotech.api.domain.product.dto.CreateVariantCommand;
import com.nitrotech.api.domain.product.dto.ProductVariantData;
import com.nitrotech.api.domain.product.exception.ProductNotFoundException;
import com.nitrotech.api.domain.product.exception.VariantSkuExistsException;
import com.nitrotech.api.domain.product.repository.ProductRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class CreateVariantUseCase {

    private final ProductRepository productRepository;

    public ProductVariantData execute(Long productId, CreateVariantCommand command) {
        if (!productRepository.existsById(productId)) {
            throw new ProductNotFoundException();
        }
        if (productRepository.existsBySku(command.sku())) {
            throw new VariantSkuExistsException();
        }
        return productRepository.createVariant(productId, command);
    }
}
