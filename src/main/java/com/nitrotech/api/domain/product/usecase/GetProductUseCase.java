package com.nitrotech.api.domain.product.usecase;

import com.nitrotech.api.domain.product.dto.ProductData;
import com.nitrotech.api.domain.product.repository.ProductRepository;
import com.nitrotech.api.shared.exception.NotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class GetProductUseCase {

    private final ProductRepository productRepository;

    /**
     * Get product by ID or slug (auto-detect)
     * Supports: numeric ID ("123"), slug ("macbook-pro-m4"), or slug-i.ID format ("macbook-pro-m4-i.123")
     */
    public ProductData execute(String idOrSlug) {
        if (idOrSlug.contains("-i.")) {
            String[] parts = idOrSlug.split("-i\\.");
            if (parts.length == 2) {
                try {
                    Long id = Long.parseLong(parts[1]);
                    return productRepository.findNotDeletedById(id)
                            .orElseThrow(() -> new NotFoundException("PRODUCT_NOT_FOUND", 
                                    "Product with ID " + id + " not found"));
                } catch (NumberFormatException ignored) {
                }
            }
        }
        
        try {
            Long id = Long.parseLong(idOrSlug);
            return productRepository.findNotDeletedById(id)
                    .orElseThrow(() -> new NotFoundException("PRODUCT_NOT_FOUND", 
                            "Product with ID " + id + " not found"));
        } catch (NumberFormatException e) {
            return productRepository.findNotDeletedBySlug(idOrSlug)
                    .orElseThrow(() -> new NotFoundException("PRODUCT_NOT_FOUND", 
                            "Product with slug '" + idOrSlug + "' not found"));
        }
    }
}
