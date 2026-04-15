package com.nitrotech.api.domain.product.repository;

import com.nitrotech.api.domain.product.dto.*;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.Optional;

public interface ProductRepository {
    ProductData create(CreateProductCommand command);
    ProductData update(UpdateProductCommand command);
    Optional<ProductData> findById(Long id);
    Page<ProductData> findAll(ProductFilter filter, Pageable pageable);
    boolean existsById(Long id);
    boolean existsBySlug(String slug);
    boolean existsBySlugAndIdNot(String slug, Long id);
    void softDelete(Long id);

    // variants
    ProductVariantData createVariant(Long productId, CreateVariantCommand command);
    ProductVariantData updateVariant(UpdateVariantCommand command);
    Optional<ProductVariantData> findVariantById(Long id);
    boolean existsVariantById(Long id);
    boolean existsBySku(String sku);
    boolean existsBySkuAndIdNot(String sku, Long id);
    void softDeleteVariant(Long id);
}
