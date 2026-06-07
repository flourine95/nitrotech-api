package com.nitrotech.api.domain.product.usecase;

import com.nitrotech.api.domain.product.dto.ProductData;
import com.nitrotech.api.domain.product.repository.ProductRepository;
import com.nitrotech.api.shared.exception.NotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class GetPublicProductUseCase {

    private final ProductRepository productRepository;

    public ProductData execute(String idOrSlug) {
        if (idOrSlug.contains("-i.")) {
            String[] parts = idOrSlug.split("-i\\.");
            if (parts.length == 2) {
                try {
                    Long id = Long.parseLong(parts[1]);
                    return productRepository.findVisibleById(id)
                            .orElseThrow(() -> notFound(idOrSlug));
                } catch (NumberFormatException ignored) {
                }
            }
        }

        try {
            Long id = Long.parseLong(idOrSlug);
            return productRepository.findVisibleById(id)
                    .orElseThrow(() -> notFound(idOrSlug));
        } catch (NumberFormatException e) {
            return productRepository.findVisibleBySlug(idOrSlug)
                    .orElseThrow(() -> notFound(idOrSlug));
        }
    }

    private NotFoundException notFound(String idOrSlug) {
        return new NotFoundException("PRODUCT_NOT_FOUND", "Product '" + idOrSlug + "' not found");
    }
}
