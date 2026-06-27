package com.nitrotech.api.domain.product.usecase;

import com.nitrotech.api.domain.product.exception.VariantSkuExistsException;

import com.nitrotech.api.domain.product.exception.VariantNotFoundException;

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
            throw new VariantNotFoundException();
        }
        if (command.sku() != null && productRepository.existsBySkuAndIdNot(command.sku(), command.id())) {
            throw new VariantSkuExistsException();
        }
        return productRepository.updateVariant(command);
    }
}
