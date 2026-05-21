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
     * Supports formats:
     * - Numeric ID: "123"
     * - Pure slug: "macbook-pro-m4"
     * - Slug with ID: "macbook-pro-m4-i.123" (Shopee/Lazada style)
     * 
     * @param idOrSlug numeric ID, slug, or slug-i.ID format
     */
    public ProductData execute(String idOrSlug) {
        // Check for "slug-i.ID" format (e.g., "macbook-pro-m4-i.123")
        if (idOrSlug.contains("-i.")) {
            String[] parts = idOrSlug.split("-i\\.");
            if (parts.length == 2) {
                try {
                    Long id = Long.parseLong(parts[1]);
                    return productRepository.findById(id)
                            .orElseThrow(() -> new NotFoundException("PRODUCT_NOT_FOUND", "Product not found"));
                } catch (NumberFormatException ignored) {
                    // Fall through to slug lookup
                }
            }
        }
        
        // Try to parse as Long (ID)
        try {
            Long id = Long.parseLong(idOrSlug);
            return productRepository.findById(id)
                    .orElseThrow(() -> new NotFoundException("PRODUCT_NOT_FOUND", "Product not found"));
        } catch (NumberFormatException e) {
            // Not a number, treat as slug
            return productRepository.findBySlug(idOrSlug)
                    .orElseThrow(() -> new NotFoundException("PRODUCT_NOT_FOUND", "Product not found"));
        }
    }

    /**
     * Get product by ID (deprecated, use execute(String) instead)
     */
    @Deprecated
    public ProductData execute(Long id) {
        return productRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("PRODUCT_NOT_FOUND", "Product not found"));
    }

    /**
     * Get product by slug (deprecated, use execute(String) instead)
     */
    @Deprecated
    public ProductData executeBySlug(String slug) {
        return productRepository.findBySlug(slug)
                .orElseThrow(() -> new NotFoundException("PRODUCT_NOT_FOUND", "Product not found"));
    }
}
