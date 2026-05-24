package com.nitrotech.api.domain.product.usecase;

import com.nitrotech.api.domain.product.dto.ProductPickerItem;
import com.nitrotech.api.domain.product.repository.ProductRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class SearchProductsUseCase {

    private final ProductRepository productRepository;

    public List<ProductPickerItem> execute(
            String search,
            String categorySlug,
            String brandSlug,
            List<Long> excludeIds,
            int limit
    ) {
        Pageable pageable = PageRequest.of(0, limit);
        return productRepository.search(search, categorySlug, brandSlug, excludeIds, pageable);
    }
}
