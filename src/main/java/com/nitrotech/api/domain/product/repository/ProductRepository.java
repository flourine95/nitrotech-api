package com.nitrotech.api.domain.product.repository;

import com.nitrotech.api.domain.product.dto.*;

import java.util.List;
import java.util.Optional;

public interface ProductRepository {
    ProductData create(CreateProductCommand command);
    ProductData update(UpdateProductCommand command);
    Optional<ProductData> findById(Long id);
    List<ProductData> findAll(ProductListQuery query);
    long countAll(ProductListQuery query);
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
