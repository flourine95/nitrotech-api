package com.nitrotech.api.infrastructure.persistence.repository;

import com.nitrotech.api.domain.product.dto.*;
import com.nitrotech.api.domain.product.repository.ProductRepository;
import com.nitrotech.api.infrastructure.persistence.entity.*;
import com.nitrotech.api.infrastructure.persistence.spec.ProductSpecification;
import com.nitrotech.api.shared.exception.NotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;

@Repository
@RequiredArgsConstructor
public class ProductRepositoryImpl implements ProductRepository {

    private final ProductJpaRepository productJpa;
    private final ProductImageJpaRepository imageJpa;
    private final ProductVariantJpaRepository variantJpa;
    private final CategoryJpaRepository categoryJpa;
    private final BrandJpaRepository brandJpa;
    private final ReviewJpaRepository reviewJpa;

    @Override
    @Transactional
    public ProductData create(CreateProductCommand command) {
        ProductEntity entity = new ProductEntity();
        entity.setCategoryId(command.categoryId());
        entity.setBrandId(command.brandId());
        entity.setName(command.name());
        entity.setSlug(command.slug());
        entity.setDescription(command.description());
        entity.setThumbnail(command.thumbnail());
        entity.setSpecs(command.specs());
        entity.setActive(command.active());
        entity.setManualBadge(command.manualBadge());
        entity.setManualBadgeExpiresAt(command.manualBadgeExpiresAt());
        ProductEntity saved = productJpa.save(entity);

        saveImages(saved.getId(), command.images());

        if (command.variants() != null) {
            command.variants().forEach(v -> saveVariant(saved.getId(), v));
        }

        return toDetailData(saved);
    }

    @Override
    @Transactional
    public ProductData update(UpdateProductCommand command) {
        ProductEntity entity = productJpa.findActiveById(command.id())
                .orElseThrow(() -> new NotFoundException("PRODUCT_NOT_FOUND", "Product not found"));
        if (command.categoryId() != null) entity.setCategoryId(command.categoryId());
        if (command.brandId() != null) entity.setBrandId(command.brandId());
        if (command.name() != null) entity.setName(command.name());
        if (command.slug() != null) entity.setSlug(command.slug());
        if (command.description() != null) entity.setDescription(command.description());
        if (command.thumbnail() != null) entity.setThumbnail(command.thumbnail());
        if (command.specs() != null) entity.setSpecs(command.specs());
        if (command.active() != null) entity.setActive(command.active());
        if (command.manualBadge() != null || command.manualBadgeExpiresAt() != null) {
            entity.setManualBadge(command.manualBadge());
            entity.setManualBadgeExpiresAt(command.manualBadgeExpiresAt());
        }
        entity.setUpdatedAt(LocalDateTime.now());
        ProductEntity saved = productJpa.save(entity);

        if (command.images() != null) {
            imageJpa.deleteByProductId(saved.getId());
            saveImages(saved.getId(), command.images());
        }

        return toDetailData(saved);
    }

    @Override
    public Optional<ProductData> findById(Long id) {
        return productJpa.findActiveById(id).map(this::toDetailData);
    }

    @Override
    public Optional<ProductData> findBySlug(String slug) {
        return productJpa.findBySlugAndDeletedAtIsNull(slug).map(this::toDetailData);
    }

    @Override
    public Page<ProductData> findAll(ProductFilter filter, Pageable pageable) {
        Page<ProductEntity> page = productJpa.findAll(ProductSpecification.from(filter), pageable);
        
        if (page.isEmpty()) {
            return page.map(this::toListData);
        }
        
        List<Long> productIds = page.getContent().stream().map(ProductEntity::getId).toList();
        var imagesMap = batchLoadImages(productIds);
        var statsMap = batchLoadProductStats(productIds);
        var reviewStatsMap = batchLoadReviewStats(productIds);
        var categoryNames = batchLoadCategoryNames(
                page.getContent().stream().map(ProductEntity::getCategoryId).filter(Objects::nonNull).distinct().toList()
        );
        var brandNames = batchLoadBrandNames(
                page.getContent().stream().map(ProductEntity::getBrandId).filter(id -> id != null).distinct().toList()
        );
        
        return page.map(e -> toListDataBatched(e, imagesMap, statsMap, reviewStatsMap, categoryNames, brandNames));
    }

    @Override
    public boolean existsById(Long id) { return productJpa.existsActiveById(id); }

    @Override
    public boolean existsBySlug(String slug) { return productJpa.existsBySlug(slug); }

    @Override
    public boolean existsBySlugAndIdNot(String slug, Long id) { return productJpa.existsBySlugAndIdNot(slug, id); }

    @Override
    public void softDelete(Long id) {
        productJpa.findActiveById(id).ifPresent(e -> {
            e.setDeletedAt(LocalDateTime.now());
            productJpa.save(e);
        });
    }

    @Override
    public ProductVariantData createVariant(Long productId, CreateVariantCommand command) {
        return toVariantData(saveVariant(productId, command));
    }

    @Override
    public ProductVariantData updateVariant(UpdateVariantCommand command) {
        ProductVariantEntity entity = variantJpa.findActiveById(command.id())
                .orElseThrow(() -> new NotFoundException("VARIANT_NOT_FOUND", "Variant not found"));
        if (command.sku() != null) entity.setSku(command.sku());
        if (command.name() != null) entity.setName(command.name());
        if (command.price() != null) entity.setPrice(command.price());
        if (command.attributes() != null) entity.setAttributes(command.attributes());
        if (command.active() != null) entity.setActive(command.active());
        if (command.imageId() != null) entity.setImageId(command.imageId());
        entity.setUpdatedAt(LocalDateTime.now());
        return toVariantData(variantJpa.save(entity));
    }

    @Override
    public Optional<ProductVariantData> findVariantById(Long id) {
        return variantJpa.findActiveById(id).map(this::toVariantData);
    }

    @Override
    public boolean existsVariantById(Long id) { return variantJpa.existsActiveById(id); }

    @Override
    public boolean existsBySku(String sku) { return variantJpa.existsBySku(sku); }

    @Override
    public boolean existsBySkuAndIdNot(String sku, Long id) { return variantJpa.existsBySkuAndIdNot(sku, id); }

    @Override
    public Optional<ProductData> findDeletedById(Long id) {
        return productJpa.findDeletedById(id).map(this::toDetailData);
    }

    @Override
    public void restore(Long id) {
        productJpa.findDeletedById(id).ifPresent(e -> {
            e.setDeletedAt(null);
            productJpa.save(e);
        });
    }

    @Override
    public void hardDelete(Long id) {
        productJpa.deleteById(id);
    }

    @Override
    public void softDeleteVariant(Long id) {
        variantJpa.findActiveById(id).ifPresent(e -> {
            e.setDeletedAt(LocalDateTime.now());
            variantJpa.save(e);
        });
    }

    private Double roundRating(Double rating) {
        if (rating == null) return null;
        return Math.round(rating * 10.0) / 10.0;
    }

    private void saveImages(Long productId, List<String> urls) {
        if (urls == null) return;
        for (int i = 0; i < urls.size(); i++) {
            ProductImageEntity img = new ProductImageEntity();
            img.setProductId(productId);
            img.setUrl(urls.get(i));
            img.setSortOrder(i);
            imageJpa.save(img);
        }
    }

    private ProductVariantEntity saveVariant(Long productId, CreateVariantCommand command) {
        ProductVariantEntity entity = new ProductVariantEntity();
        entity.setProductId(productId);
        entity.setSku(command.sku());
        entity.setName(command.name());
        entity.setPrice(command.price());
        entity.setAttributes(command.attributes());
        entity.setActive(command.active());
        entity.setImageId(command.imageId());
        return variantJpa.save(entity);
    }

    /** Dùng cho list endpoint — không load variants, dùng aggregate query */
    private ProductData toListData(ProductEntity e) {
        List<String> images = imageJpa.findByProductIdOrderBySortOrderAsc(e.getId())
                .stream().map(ProductImageEntity::getUrl).toList();
        int variantCount = productJpa.countActiveVariants(e.getId());
        
        String badge = computeBadge(e, variantCount);
        
        Object[] reviewStats = reviewJpa.getReviewStats(e.getId());
        Double rating = null;
        Integer reviewCount = null;
        
        if (reviewStats != null && reviewStats.length >= 2) {
            if (reviewStats[0] instanceof Number) {
                double rawRating = ((Number) reviewStats[0]).doubleValue();
                rating = rawRating > 0 ? roundRating(rawRating) : null;
            }
            if (reviewStats[1] instanceof Number) {
                reviewCount = ((Number) reviewStats[1]).intValue();
                if (reviewCount == 0) reviewCount = null;
            }
        }
        
        return new ProductData(
                e.getId(), e.getCategoryId(), resolveCategoryName(e.getCategoryId()),
                e.getBrandId(), resolveBrandName(e.getBrandId()),
                e.getName(), e.getSlug(), e.getDescription(), e.getThumbnail(),
                e.getSpecs(), e.isActive(), images,
                null,
                variantCount,
                productJpa.findMinPrice(e.getId()),
                productJpa.findMaxPrice(e.getId()),
                badge,
                rating,
                reviewCount,
                e.getCreatedAt(), e.getUpdatedAt()
        );
    }

    /** Dùng cho detail endpoint — load full variants */
    private ProductData toDetailData(ProductEntity e) {
        List<String> images = imageJpa.findByProductIdOrderBySortOrderAsc(e.getId())
                .stream().map(ProductImageEntity::getUrl).toList();
        List<ProductVariantData> variants = variantJpa.findActiveByProductId(e.getId())
                .stream().map(this::toVariantData).toList();
        
        String badge = computeBadge(e, variants.size());
        
        Map<Long, ReviewStats> reviewStatsMap = batchLoadReviewStats(List.of(e.getId()));
        ReviewStats reviewStats = reviewStatsMap.get(e.getId());
        
        Double rating = reviewStats != null ? reviewStats.rating() : null;
        Integer reviewCount = reviewStats != null ? reviewStats.reviewCount() : null;
        
        return new ProductData(
                e.getId(), e.getCategoryId(), resolveCategoryName(e.getCategoryId()),
                e.getBrandId(), resolveBrandName(e.getBrandId()),
                e.getName(), e.getSlug(), e.getDescription(), e.getThumbnail(),
                e.getSpecs(), e.isActive(), images,
                variants,
                variants.size(),
                variants.stream().map(ProductVariantData::price)
                        .filter(p -> p != null)
                        .min(BigDecimal::compareTo).orElse(null),
                variants.stream().map(ProductVariantData::price)
                        .filter(p -> p != null)
                        .max(BigDecimal::compareTo).orElse(null),
                badge,
                rating,
                reviewCount,
                e.getCreatedAt(), e.getUpdatedAt()
        );
    }

    private String resolveCategoryName(Long categoryId) {
        if (categoryId == null) return null;
        return categoryJpa.findById(categoryId).map(c -> c.getName()).orElse(null);
    }

    private String resolveBrandName(Long brandId) {
        if (brandId == null) return null;
        return brandJpa.findById(brandId).map(b -> b.getName()).orElse(null);
    }

    private ProductVariantData toVariantData(ProductVariantEntity e) {
        String imageUrl = null;
        if (e.getImageId() != null) {
            imageUrl = imageJpa.findById(e.getImageId())
                    .map(ProductImageEntity::getUrl)
                    .orElse(null);
        }
        
        return new ProductVariantData(
                e.getId(), e.getProductId(), e.getSku(), e.getName(),
                e.getPrice(), e.getAttributes(), e.isActive(),
                e.getImageId(), imageUrl,
                e.getCreatedAt(), e.getUpdatedAt()
        );
    }

    private String computeBadge(ProductEntity product, int variantCount) {
        LocalDateTime now = LocalDateTime.now();
        
        if (product.getManualBadge() != null) {
            if (product.getManualBadgeExpiresAt() == null || 
                product.getManualBadgeExpiresAt().isAfter(now)) {
                return product.getManualBadge();
            }
        }
        
        if (variantCount > 0 && variantCount <= 5) {
            return "lowstock";
        }
        
        if (product.getCreatedAt().isAfter(now.minusDays(30))) {
            return "new";
        }
        
        return null;
    }

    private Map<Long, List<String>> batchLoadImages(List<Long> productIds) {
        return imageJpa.findByProductIdInOrderBySortOrderAsc(productIds).stream()
                .collect(Collectors.groupingBy(
                        ProductImageEntity::getProductId,
                        Collectors.mapping(ProductImageEntity::getUrl, Collectors.toList())
                ));
    }

    private Map<Long, ProductStats> batchLoadProductStats(List<Long> productIds) {
        var variantCounts = productJpa.countActiveVariantsBatch(productIds);
        var minPrices = productJpa.findMinPricesBatch(productIds);
        var maxPrices = productJpa.findMaxPricesBatch(productIds);
        
        Map<Long, ProductStats> result = new HashMap<>();
        for (Long id : productIds) {
            result.put(id, new ProductStats(
                    variantCounts.getOrDefault(id, 0),
                    minPrices.get(id),
                    maxPrices.get(id)
            ));
        }
        return result;
    }

    private Map<Long, ReviewStats> batchLoadReviewStats(List<Long> productIds) {
        List<Object[]> results = reviewJpa.getReviewStatsBatch(productIds);
        Map<Long, ReviewStats> map = new HashMap<>();
        
        for (Object[] row : results) {
            if (row != null && row.length >= 3) {
                Long productId = row[0] instanceof Number ? ((Number) row[0]).longValue() : null;
                if (productId == null) continue;
                
                Double avgRating = null;
                if (row[1] instanceof Number) {
                    double rawRating = ((Number) row[1]).doubleValue();
                    avgRating = rawRating > 0 ? roundRating(rawRating) : null;
                }
                
                Integer reviewCount = null;
                if (row[2] instanceof Number) {
                    int count = ((Number) row[2]).intValue();
                    reviewCount = count > 0 ? count : null;
                }
                
                map.put(productId, new ReviewStats(avgRating, reviewCount));
            }
        }
        
        return map;
    }

    private Map<Long, String> batchLoadCategoryNames(List<Long> categoryIds) {
        if (categoryIds.isEmpty()) return Map.of();
        return categoryJpa.findAllById(categoryIds).stream()
                .collect(Collectors.toMap(
                        CategoryEntity::getId,
                        CategoryEntity::getName
                ));
    }

    private Map<Long, String> batchLoadBrandNames(List<Long> brandIds) {
        if (brandIds.isEmpty()) return Map.of();
        return brandJpa.findAllById(brandIds).stream()
                .collect(Collectors.toMap(
                        BrandEntity::getId,
                        BrandEntity::getName
                ));
    }

    private ProductData toListDataBatched(
            ProductEntity e,
            Map<Long, List<String>> imagesMap,
            Map<Long, ProductStats> statsMap,
            Map<Long, ReviewStats> reviewStatsMap,
            Map<Long, String> categoryNames,
            Map<Long, String> brandNames
    ) {
        List<String> images = imagesMap.getOrDefault(e.getId(), List.of());
        ProductStats stats = statsMap.getOrDefault(e.getId(), new ProductStats(0, null, null));
        ReviewStats reviewStats = reviewStatsMap.getOrDefault(e.getId(), new ReviewStats(null, null));
        
        String badge = computeBadge(e, stats.variantCount);
        
        return new ProductData(
                e.getId(), e.getCategoryId(), categoryNames.get(e.getCategoryId()),
                e.getBrandId(), brandNames.get(e.getBrandId()),
                e.getName(), e.getSlug(), e.getDescription(), e.getThumbnail(),
                e.getSpecs(), e.isActive(), images,
                null,
                stats.variantCount,
                stats.minPrice,
                stats.maxPrice,
                badge,
                reviewStats.rating,
                reviewStats.reviewCount,
                e.getCreatedAt(), e.getUpdatedAt()
        );
    }

    private record ProductStats(int variantCount, BigDecimal minPrice, BigDecimal maxPrice) {}
    private record ReviewStats(Double rating, Integer reviewCount) {}
}
