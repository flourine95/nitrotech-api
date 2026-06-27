package com.nitrotech.api.domain.product.repository;

import com.nitrotech.api.domain.product.dto.*;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.List;
import java.util.Optional;

public interface ProductRepository {
    ProductData create(CreateProductCommand command);

    ProductData update(UpdateProductCommand command);

    Optional<ProductData> findNotDeletedById(Long id);

    Optional<ProductData> findNotDeletedBySlug(String slug);

    Optional<ProductData> findVisibleById(Long id);

    Optional<ProductData> findVisibleBySlug(String slug);

    Page<ProductData> findAll(ProductFilter filter, Pageable pageable);

    Page<ProductData> findAllSortedByPrice(ProductFilter filter, Pageable pageable);

    boolean existsById(Long id);

    boolean existsNotDeletedBySlug(String slug);

    boolean existsNotDeletedBySlugAndIdNot(String slug, Long id);

    Optional<ProductData> findDeletedById(Long id);

    void softDelete(Long id);

    void restore(Long id);

    void hardDelete(Long id);

    List<ProductPickerItem> search(String search, String categorySlug, String brandSlug, List<Long> excludeIds, Pageable pageable);

    List<ProductData> findRelated(Long productId, int limit);

    ProductFacets getFacets(ProductFilter filter);

    ProductVariantData createVariant(Long productId, CreateVariantCommand command);

    ProductVariantData updateVariant(UpdateVariantCommand command);

    Optional<ProductVariantData> findVariantById(Long id);

    boolean existsVariantById(Long id);

    boolean existsBySku(String sku);

    boolean existsBySkuAndIdNot(String sku, Long id);

    void softDeleteVariant(Long id);
}
