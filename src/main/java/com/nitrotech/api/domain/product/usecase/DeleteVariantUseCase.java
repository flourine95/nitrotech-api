package com.nitrotech.api.domain.product.usecase;

import com.nitrotech.api.domain.product.repository.ProductRepository;
import com.nitrotech.api.shared.exception.NotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class DeleteVariantUseCase {

    private final ProductRepository productRepository;

    public void execute(Long id) {
        if (!productRepository.existsVariantById(id)) {
            throw new NotFoundException("VARIANT_NOT_FOUND", "Variant not found");
        }
        productRepository.softDeleteVariant(id);
    }
}
