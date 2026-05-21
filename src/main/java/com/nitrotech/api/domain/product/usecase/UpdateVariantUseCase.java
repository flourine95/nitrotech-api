package com.nitrotech.api.domain.product.usecase;

import com.nitrotech.api.domain.product.dto.ProductVariantData;
import com.nitrotech.api.domain.product.dto.UpdateVariantCommand;
import com.nitrotech.api.domain.product.repository.ProductRepository;
import com.nitrotech.api.shared.exception.ConflictException;
import com.nitrotech.api.shared.exception.NotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class UpdateVariantUseCase {

    private final ProductRepository productRepository;

    public ProductVariantData execute(UpdateVariantCommand command) {
        if (!productRepository.existsVariantById(command.id())) {
            throw new NotFoundException("VARIANT_NOT_FOUND", "Variant not found");
        }
        if (command.sku() != null && productRepository.existsBySkuAndIdNot(command.sku(), command.id())) {
            throw new ConflictException("VARIANT_SKU_EXISTS", "SKU already exists");
        }
        return productRepository.updateVariant(command);
    }
}
