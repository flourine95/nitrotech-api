package com.nitrotech.api.infrastructure.persistence.repository;

import com.nitrotech.api.domain.product.dto.*;
import com.nitrotech.api.domain.product.repository.ProductRepository;
import com.nitrotech.api.infrastructure.persistence.entity.*;
import com.nitrotech.api.infrastructure.persistence.spec.ProductSpecification;
import com.nitrotech.api.shared.exception.NotFoundException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public class ProductRepositoryImpl implements ProductRepository {

    private final ProductJpaRepository productJpa;
    private final ProductImageJpaRepository imageJpa;
    private final ProductVariantJpaRepository variantJpa;
    private final CategoryJpaRepository categoryJpa;
    private final BrandJpaRepository brandJpa;

    public ProductRepositoryImpl(ProductJpaRepository productJpa,
                                  ProductImageJpaRepository imageJpa,
                                  ProductVariantJpaRepository variantJpa,
                                  CategoryJpaRepository categoryJpa,
                                  BrandJpaRepository brandJpa) {
        this.productJpa = productJpa;
        this.imageJpa = imageJpa;
        this.variantJpa = variantJpa;
        this.categoryJpa = categoryJpa;
        this.brandJpa = brandJpa;
    }

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
            return page.map(this::toListData); // fallback nếu rỗng
        }
        
        // Batch query tất cả data cần thiết
        List<Long> productIds = page.getContent().stream().map(ProductEntity::getId).toList();
        var imagesMap = batchLoadImages(productIds);
        var statsMap = batchLoadProductStats(productIds);
        var categoryNames = batchLoadCategoryNames(
                page.getContent().stream().map(ProductEntity::getCategoryId).filter(id -> id != null).distinct().toList()
        );
        var brandNames = batchLoadBrandNames(
                page.getContent().stream().map(ProductEntity::getBrandId).filter(id -> id != null).distinct().toList()
        );
        
        // Map nhanh từ cache
        return page.map(e -> toListDataBatched(e, imagesMap, statsMap, categoryNames, brandNames));
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

    // ── helpers ───────────────────────────────────────────────────────────────

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
        
        // Compute badge
        String badge = computeBadge(e, variantCount);
        
        return new ProductData(
                e.getId(), e.getCategoryId(), resolveCategoryName(e.getCategoryId()),
                e.getBrandId(), resolveBrandName(e.getBrandId()),
                e.getName(), e.getSlug(), e.getDescription(), e.getThumbnail(),
                e.getSpecs(), e.isActive(), images,
                null,                                        // variants — không load trong list
                variantCount,
                productJpa.findMinPrice(e.getId()),
                productJpa.findMaxPrice(e.getId()),
                badge,
                null,  // rating - TODO: aggregate from reviews
                null,  // reviewCount - TODO: aggregate from reviews
                e.getCreatedAt(), e.getUpdatedAt()
        );
    }

    /** Dùng cho detail endpoint — load full variants */
    private ProductData toDetailData(ProductEntity e) {
        List<String> images = imageJpa.findByProductIdOrderBySortOrderAsc(e.getId())
                .stream().map(ProductImageEntity::getUrl).toList();
        List<ProductVariantData> variants = variantJpa.findActiveByProductId(e.getId())
                .stream().map(this::toVariantData).toList();
        
        // Compute badge
        String badge = computeBadge(e, variants.size());
        
        return new ProductData(
                e.getId(), e.getCategoryId(), resolveCategoryName(e.getCategoryId()),
                e.getBrandId(), resolveBrandName(e.getBrandId()),
                e.getName(), e.getSlug(), e.getDescription(), e.getThumbnail(),
                e.getSpecs(), e.isActive(), images,
                variants,
                variants.size(),
                variants.stream().map(ProductVariantData::price)
                        .filter(p -> p != null)
                        .min(java.math.BigDecimal::compareTo).orElse(null),
                variants.stream().map(ProductVariantData::price)
                        .filter(p -> p != null)
                        .max(java.math.BigDecimal::compareTo).orElse(null),
                badge,
                null,  // rating - TODO: aggregate from reviews
                null,  // reviewCount - TODO: aggregate from reviews
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

    // ── Badge computation ─────────────────────────────────────────────────────

    /**
     * Compute badge for product with priority system:
     * 1. Manual badge (if set and not expired) - highest priority
     * 2. Auto-computed badges - fallback
     *    - lowstock: variantCount <= 5
     *    - bestseller: TODO (requires order_items data)
     *    - new: created < 30 days ago
     */
    private String computeBadge(ProductEntity product, int variantCount) {
        LocalDateTime now = LocalDateTime.now();
        
        // 1️⃣ Manual badge (highest priority)
        if (product.getManualBadge() != null) {
            // Check if expired
            if (product.getManualBadgeExpiresAt() == null || 
                product.getManualBadgeExpiresAt().isAfter(now)) {
                return product.getManualBadge();
            }
            // Expired → fallback to auto badge
        }
        
        // 2️⃣ Auto-computed badges (fallback)
        
        // Low stock (sắp hết) - Priority 1
        if (variantCount > 0 && variantCount <= 5) {
            return "lowstock";
        }
        
        // Best seller (bán chạy) - Priority 2
        // TODO: Implement when order_items data available
        // int sold30Days = getSoldLast30Days(product.getId());
        // if (sold30Days >= 100) return "bestseller";
        
        // New (mới) - Priority 3
        if (product.getCreatedAt().isAfter(now.minusDays(30))) {
            return "new";
        }
        
        return null;
    }

    // ── Batch loading helpers ─────────────────────────────────────────────────

    private java.util.Map<Long, List<String>> batchLoadImages(List<Long> productIds) {
        return imageJpa.findByProductIdInOrderBySortOrderAsc(productIds).stream()
                .collect(java.util.stream.Collectors.groupingBy(
                        ProductImageEntity::getProductId,
                        java.util.stream.Collectors.mapping(ProductImageEntity::getUrl, java.util.stream.Collectors.toList())
                ));
    }

    private java.util.Map<Long, ProductStats> batchLoadProductStats(List<Long> productIds) {
        // Query 1 lần cho tất cả products
        var variantCounts = productJpa.countActiveVariantsBatch(productIds);
        var minPrices = productJpa.findMinPricesBatch(productIds);
        var maxPrices = productJpa.findMaxPricesBatch(productIds);
        
        java.util.Map<Long, ProductStats> result = new java.util.HashMap<>();
        for (Long id : productIds) {
            result.put(id, new ProductStats(
                    variantCounts.getOrDefault(id, 0),
                    minPrices.get(id),
                    maxPrices.get(id)
            ));
        }
        return result;
    }

    private java.util.Map<Long, String> batchLoadCategoryNames(List<Long> categoryIds) {
        if (categoryIds.isEmpty()) return java.util.Map.of();
        return categoryJpa.findAllById(categoryIds).stream()
                .collect(java.util.stream.Collectors.toMap(
                        CategoryEntity::getId,
                        CategoryEntity::getName
                ));
    }

    private java.util.Map<Long, String> batchLoadBrandNames(List<Long> brandIds) {
        if (brandIds.isEmpty()) return java.util.Map.of();
        return brandJpa.findAllById(brandIds).stream()
                .collect(java.util.stream.Collectors.toMap(
                        BrandEntity::getId,
                        BrandEntity::getName
                ));
    }

    private ProductData toListDataBatched(
            ProductEntity e,
            java.util.Map<Long, List<String>> imagesMap,
            java.util.Map<Long, ProductStats> statsMap,
            java.util.Map<Long, String> categoryNames,
            java.util.Map<Long, String> brandNames
    ) {
        List<String> images = imagesMap.getOrDefault(e.getId(), List.of());
        ProductStats stats = statsMap.getOrDefault(e.getId(), new ProductStats(0, null, null));
        
        // Compute badge
        String badge = computeBadge(e, stats.variantCount);
        
        return new ProductData(
                e.getId(), e.getCategoryId(), categoryNames.get(e.getCategoryId()),
                e.getBrandId(), brandNames.get(e.getBrandId()),
                e.getName(), e.getSlug(), e.getDescription(), e.getThumbnail(),
                e.getSpecs(), e.isActive(), images,
                null, // variants — không load trong list
                stats.variantCount,
                stats.minPrice,
                stats.maxPrice,
                badge,
                null,  // rating - TODO
                null,  // reviewCount - TODO
                e.getCreatedAt(), e.getUpdatedAt()
        );
    }

    private record ProductStats(int variantCount, java.math.BigDecimal minPrice, java.math.BigDecimal maxPrice) {}
}
