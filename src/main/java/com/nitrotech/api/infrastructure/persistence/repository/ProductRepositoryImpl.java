package com.nitrotech.api.infrastructure.persistence.repository;

import com.nitrotech.api.domain.product.dto.*;
import com.nitrotech.api.domain.product.repository.ProductRepository;
import com.nitrotech.api.infrastructure.persistence.entity.*;
import com.nitrotech.api.infrastructure.persistence.spec.ProductSpecification;
import com.nitrotech.api.shared.exception.NotFoundException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;

@Slf4j
@Repository
@RequiredArgsConstructor
public class ProductRepositoryImpl implements ProductRepository {

    private final ProductJpaRepository productJpa;
    private final ProductImageJpaRepository imageJpa;
    private final ProductVariantJpaRepository variantJpa;
    private final CategoryJpaRepository categoryJpa;
    private final BrandJpaRepository brandJpa;
    private final ReviewJpaRepository reviewJpa;
    private final InventoryJpaRepository inventoryJpa;

    @Override
    @Transactional
    public ProductData create(CreateProductCommand command) {
        ProductEntity entity = new ProductEntity();
        entity.setCategoryId(command.categoryId());
        entity.setBrandId(command.brandId());
        entity.setName(command.name());
        entity.setSlug(command.slug());
        entity.setDescription(command.description());
        entity.setShortDescription(command.shortDescription());
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
        if (command.shortDescription() != null) entity.setShortDescription(command.shortDescription());
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
        return productJpa.findActiveByIdWithRelations(id).map(this::toDetailData);
    }

    @Override
    public Optional<ProductData> findBySlug(String slug) {
        return productJpa.findBySlugWithRelations(slug).map(this::toDetailData);
    }

    @Override
    public Optional<ProductData> findVisibleById(Long id) {
        return productJpa.findVisibleByIdWithRelations(id).map(this::toDetailData);
    }

    @Override
    public Optional<ProductData> findVisibleBySlug(String slug) {
        return productJpa.findVisibleBySlugWithRelations(slug).map(this::toDetailData);
    }

    @Override
    public Page<ProductData> findAll(ProductFilter filter, Pageable pageable) {
        Page<ProductEntity> page = productJpa.findAll(
                ProductSpecification.from(filter, resolveCategoryIds(filter.categorySlug())),
                pageable
        );
        
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
        var categorySlugs = batchLoadCategorySlugs(
                page.getContent().stream().map(ProductEntity::getCategoryId).filter(Objects::nonNull).distinct().toList()
        );
        var brandNames = batchLoadBrandNames(
                page.getContent().stream().map(ProductEntity::getBrandId).filter(Objects::nonNull).distinct().toList()
        );
        
        return page.map(e -> toListDataBatched(e, imagesMap, statsMap, reviewStatsMap, categoryNames, categorySlugs, brandNames));
    }

    @Override
    public Page<ProductData> findAllSortedByPrice(ProductFilter filter, Pageable pageable) {
        boolean isAscending = pageable.getSort().stream()
                .anyMatch(order -> "price".equals(order.getProperty()) && order.isAscending());
        
        int limit = pageable.getPageSize();
        int offset = pageable.getPageNumber() * pageable.getPageSize();
        
        List<Long> productIds = isAscending
                ? productJpa.findProductIdsSortedByPriceAsc(
                        filter.active(),
                        filter.visibleRelations(),
                        filter.search(),
                        filter.categorySlug(),
                        filter.brandSlugs(),
                        filter.minPrice(),
                        filter.maxPrice(),
                        filter.badge(),
                        limit,
                        offset
                  )
                : productJpa.findProductIdsSortedByPriceDesc(
                        filter.active(),
                        filter.visibleRelations(),
                        filter.search(),
                        filter.categorySlug(),
                        filter.brandSlugs(),
                        filter.minPrice(),
                        filter.maxPrice(),
                        filter.badge(),
                        limit,
                        offset
                  );
        
        if (productIds.isEmpty()) {
            return Page.empty(pageable);
        }
        
        long total = productJpa.countProductsWithFilters(
                filter.active(),
                filter.visibleRelations(),
                filter.search(),
                filter.categorySlug(),
                filter.brandSlugs(),
                filter.minPrice(),
                filter.maxPrice(),
                filter.badge()
        );
        
        List<ProductEntity> products = productJpa.findAllById(productIds);
        Map<Long, ProductEntity> productMap = products.stream()
                .collect(Collectors.toMap(ProductEntity::getId, p -> p));
        
        List<ProductEntity> orderedProducts = productIds.stream()
                .map(productMap::get)
                .filter(Objects::nonNull)
                .toList();
        
        var imagesMap = batchLoadImages(productIds);
        var statsMap = batchLoadProductStats(productIds);
        var reviewStatsMap = batchLoadReviewStats(productIds);
        var categoryNames = batchLoadCategoryNames(
                orderedProducts.stream().map(ProductEntity::getCategoryId).filter(Objects::nonNull).distinct().toList()
        );
        var categorySlugs = batchLoadCategorySlugs(
                orderedProducts.stream().map(ProductEntity::getCategoryId).filter(Objects::nonNull).distinct().toList()
        );
        var brandNames = batchLoadBrandNames(
                orderedProducts.stream().map(ProductEntity::getBrandId).filter(Objects::nonNull).distinct().toList()
        );
        
        List<ProductData> content = orderedProducts.stream()
                .map(e -> toListDataBatched(e, imagesMap, statsMap, reviewStatsMap, categoryNames, categorySlugs, brandNames))
                .toList();
        
        return new PageImpl<>(content, pageable, total);
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

    private List<Long> resolveCategoryIds(String categorySlug) {
        if (categorySlug == null) return null;
        return categoryJpa.findDescendantIdsBySlug(categorySlug);
    }

    @Override
    public List<ProductPickerItem> search(String search, String categorySlug, String brandSlug, List<Long> excludeIds, Pageable pageable) {
        List<Object[]> results;
        
        if (excludeIds != null && !excludeIds.isEmpty()) {
            results = productJpa.searchWithExclude(search, categorySlug, brandSlug, excludeIds, pageable);
        } else {
            results = productJpa.searchWithoutExclude(search, categorySlug, brandSlug, pageable);
        }
        
        return results.stream()
                .map(this::toPickerItem)
                .toList();
    }

    @Override
    public List<ProductData> findRelated(Long productId, int limit) {
        ProductEntity current = productJpa.findVisibleByIdWithRelations(productId)
                .orElseThrow(() -> new NotFoundException("PRODUCT_NOT_FOUND", "Product not found"));

        List<ProductEntity> related = new ArrayList<>();
        List<Long> excludeIds = new ArrayList<>();
        excludeIds.add(productId);

        addRelatedBatch(
                related,
                excludeIds,
                limit,
                productJpa.findVisibleRelatedByCategory(
                        current.getCategoryId(),
                        excludeIds,
                        PageRequest.of(0, remaining(limit, related))
                )
        );

        if (current.getBrandId() != null && related.size() < limit) {
            addRelatedBatch(
                    related,
                    excludeIds,
                    limit,
                    productJpa.findVisibleRelatedByBrand(
                            current.getBrandId(),
                            excludeIds,
                            PageRequest.of(0, remaining(limit, related))
                    )
            );
        }

        if (related.size() < limit) {
            addRelatedBatch(
                    related,
                    excludeIds,
                    limit,
                    productJpa.findVisibleRelatedFallback(
                            excludeIds,
                            PageRequest.of(0, remaining(limit, related))
                    )
            );
        }

        return toListDataBatch(related);
    }

    @Override
    public ProductFacets getFacets(ProductFilter filter) {
        String categorySlug = filter.categorySlug();
        List<String> brandSlugs = filter.brandSlugs() != null && !filter.brandSlugs().isEmpty() 
                ? filter.brandSlugs() : null;

        List<CategoryFacet> categories = categorySlug == null 
                ? (brandSlugs == null 
                    ? productJpa.findCategoryFacetsWithoutBrands(
                            filter.search(), 
                            filter.minPrice(), 
                            filter.maxPrice()
                      )
                    : productJpa.findCategoryFacetsWithBrands(
                            filter.search(), 
                            brandSlugs, 
                            filter.minPrice(), 
                            filter.maxPrice()
                      )
                  ).stream().map(this::toCategoryFacet).toList()
                : List.of();

        List<BrandFacet> brands = productJpa.findBrandFacets(
                filter.search(), 
                categorySlug, 
                filter.minPrice(), 
                filter.maxPrice()
        ).stream().map(this::toBrandFacet).toList();

        List<PriceRangeFacet> priceRanges = buildPriceRanges(
                filter.search(), 
                categorySlug, 
                brandSlugs
        );

        return new ProductFacets(categories, brands, priceRanges);
    }

    private CategoryFacet toCategoryFacet(Object[] row) {
        Long id = row[0] instanceof Number number ? number.longValue() : null;
        String name = (String) row[1];
        String slug = (String) row[2];
        Integer count = row[3] instanceof Number number ? number.intValue() : 0;
        return new CategoryFacet(id, name, slug, count);
    }

    private BrandFacet toBrandFacet(Object[] row) {
        Long id = row[0] instanceof Number number ? number.longValue() : null;
        String name = (String) row[1];
        String slug = (String) row[2];
        Integer count = row[3] instanceof Number number ? number.intValue() : 0;
        return new BrandFacet(id, name, slug, count);
    }

    private List<PriceRangeFacet> buildPriceRanges(String search, String categorySlug, List<String> brandSlugs) {
        List<Object[]> results;
        try {
            results = brandSlugs == null
                    ? productJpa.findPriceRangeWithoutBrands(search, categorySlug)
                    : productJpa.findPriceRangeWithBrands(search, categorySlug, brandSlugs);
        } catch (Exception e) {
            log.error("Error fetching price range", e);
            return List.of();
        }
        
        if (results == null || results.isEmpty()) {
            log.warn("Price range query returned empty");
            return List.of();
        }
        
        Object[] priceRange = results.getFirst();
        
        if (priceRange == null || priceRange.length < 2) {
            log.warn("Price range row is invalid: length={}", priceRange == null ? "null" : priceRange.length);
            return List.of();
        }
        
        if (priceRange[0] == null || priceRange[1] == null) {
            log.warn("Price range contains null values: min={}, max={}", priceRange[0], priceRange[1]);
            return List.of();
        }

        BigDecimal[] ranges = {
                BigDecimal.ZERO,
                new BigDecimal("1000000"),
                new BigDecimal("3000000"),
                new BigDecimal("5000000"),
                new BigDecimal("10000000")
        };

        List<PriceRangeFacet> facets = new ArrayList<>();
        
        for (int i = 0; i < ranges.length; i++) {
            BigDecimal rangeMin = ranges[i];
            BigDecimal rangeMax = i < ranges.length - 1 ? ranges[i + 1] : null;
            
            int count = countProductsInPriceRange(
                    search, 
                    categorySlug, 
                    brandSlugs, 
                    rangeMin, 
                    rangeMax
            );
            
            if (count > 0) {
                facets.add(new PriceRangeFacet(rangeMin, rangeMax, count));
            }
        }
        
        return facets;
    }

    private int countProductsInPriceRange(
            String search, 
            String categorySlug, 
            List<String> brandSlugs,
            BigDecimal minPrice,
            BigDecimal maxPrice
    ) {
        List<Object[]> countResults = brandSlugs == null
                ? productJpa.countProductsInPriceRangeWithoutBrands(search, categorySlug, minPrice, maxPrice)
                : productJpa.countProductsInPriceRangeWithBrands(search, categorySlug, brandSlugs, minPrice, maxPrice);
        
        if (countResults == null || countResults.isEmpty()) {
            return 0;
        }
        
        Object[] row = countResults.getFirst();
        if (row == null || row[0] == null) {
            return 0;
        }
        
        return ((Number) row[0]).intValue();
    }

    private ProductPickerItem toPickerItem(Object[] row) {
        Long id = row[0] instanceof Number number ? number.longValue() : null;
        String slug = (String) row[1];
        String name = (String) row[2];
        String categoryName = (String) row[3];
        BigDecimal priceMin = row[4] != null ? new BigDecimal(row[4].toString()) : null;
        BigDecimal priceMax = row[5] != null ? new BigDecimal(row[5].toString()) : null;
        String thumbnail = (String) row[6];
        String badge = (String) row[7];
        
        return new ProductPickerItem(id, slug, name, categoryName, priceMin, priceMax, thumbnail, badge);
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
                e.getId(), e.getCategoryId(), resolveCategoryName(e.getCategoryId()), resolveCategorySlug(e.getCategoryId()),
                e.getBrandId(), resolveBrandName(e.getBrandId()),
                e.getName(), e.getSlug(), e.getDescription(), e.getShortDescription(), e.getThumbnail(),
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
        List<ProductVariantEntity> variantEntities = variantJpa.findActiveByProductId(e.getId());
        Map<Long, InventoryEntity> inventoryByVariantId = batchLoadInventories(variantEntities);
        List<ProductVariantData> variants = variantEntities.stream()
                .map(v -> toVariantData(v, inventoryByVariantId.get(v.getId())))
                .toList();
        
        String badge = computeBadge(e, variants.size());
        
        Map<Long, ReviewStats> reviewStatsMap = batchLoadReviewStats(List.of(e.getId()));
        ReviewStats reviewStats = reviewStatsMap.get(e.getId());
        
        Double rating = reviewStats != null ? reviewStats.rating() : null;
        Integer reviewCount = reviewStats != null ? reviewStats.reviewCount() : null;
        
        // Use relationships for category/brand
        String categoryName = e.getCategory() != null ? e.getCategory().getName() : null;
        String categorySlug = e.getCategory() != null ? e.getCategory().getSlug() : null;
        String brandName = e.getBrand() != null ? e.getBrand().getName() : null;
        
        return new ProductData(
                e.getId(), e.getCategoryId(), categoryName, categorySlug,
                e.getBrandId(), brandName,
                e.getName(), e.getSlug(), e.getDescription(), e.getShortDescription(), e.getThumbnail(),
                e.getSpecs(), e.isActive(), images,
                variants,
                variants.size(),
                variants.stream().map(ProductVariantData::price)
                        .filter(Objects::nonNull)
                        .min(BigDecimal::compareTo).orElse(null),
                variants.stream().map(ProductVariantData::price)
                        .filter(Objects::nonNull)
                        .max(BigDecimal::compareTo).orElse(null),
                badge,
                rating,
                reviewCount,
                e.getCreatedAt(), e.getUpdatedAt()
        );
    }

    private String resolveCategoryName(Long categoryId) {
        if (categoryId == null) return null;
        return categoryJpa.findById(categoryId).map(CategoryEntity::getName).orElse(null);
    }

    private String resolveCategorySlug(Long categoryId) {
        if (categoryId == null) return null;
        return categoryJpa.findById(categoryId).map(CategoryEntity::getSlug).orElse(null);
    }

    private String resolveBrandName(Long brandId) {
        if (brandId == null) return null;
        return brandJpa.findById(brandId).map(BrandEntity::getName).orElse(null);
    }

    private ProductVariantData toVariantData(ProductVariantEntity e) {
        return toVariantData(e, inventoryJpa.findByVariantId(e.getId()).orElse(null));
    }

    private ProductVariantData toVariantData(ProductVariantEntity e, InventoryEntity inventory) {
        String imageUrl = null;
        if (e.getImageId() != null) {
            imageUrl = imageJpa.findById(e.getImageId())
                    .map(ProductImageEntity::getUrl)
                    .orElse(null);
        }

        Integer stockQuantity = inventory != null ? inventory.getQuantity() : null;
        Integer lowStockThreshold = inventory != null ? inventory.getLowStockThreshold() : null;
        
        return new ProductVariantData(
                e.getId(), e.getProductId(), e.getSku(), e.getName(),
                e.getPrice(), e.getAttributes(), e.isActive(),
                e.getImageId(), imageUrl,
                stockQuantity,
                lowStockThreshold,
                stockQuantity != null ? stockQuantity > 0 : null,
                stockQuantity != null && lowStockThreshold != null ? stockQuantity <= lowStockThreshold : null,
                e.getCreatedAt(), e.getUpdatedAt()
        );
    }

    private Map<Long, InventoryEntity> batchLoadInventories(List<ProductVariantEntity> variants) {
        List<Long> variantIds = variants.stream().map(ProductVariantEntity::getId).toList();
        if (variantIds.isEmpty()) return Map.of();
        return inventoryJpa.findByVariantIdIn(variantIds).stream()
                .collect(Collectors.toMap(InventoryEntity::getVariantId, inventory -> inventory));
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

    private int remaining(int limit, List<ProductEntity> related) {
        return Math.max(0, limit - related.size());
    }

    private void addRelatedBatch(
            List<ProductEntity> related,
            List<Long> excludeIds,
            int limit,
            List<ProductEntity> candidates
    ) {
        for (ProductEntity candidate : candidates) {
            if (related.size() >= limit) return;
            if (excludeIds.contains(candidate.getId())) continue;
            related.add(candidate);
            excludeIds.add(candidate.getId());
        }
    }

    private List<ProductData> toListDataBatch(List<ProductEntity> products) {
        if (products.isEmpty()) return List.of();

        List<Long> productIds = products.stream().map(ProductEntity::getId).toList();
        var imagesMap = batchLoadImages(productIds);
        var statsMap = batchLoadProductStats(productIds);
        var reviewStatsMap = batchLoadReviewStats(productIds);
        var categoryNames = batchLoadCategoryNames(
                products.stream().map(ProductEntity::getCategoryId).filter(Objects::nonNull).distinct().toList()
        );
        var categorySlugs = batchLoadCategorySlugs(
                products.stream().map(ProductEntity::getCategoryId).filter(Objects::nonNull).distinct().toList()
        );
        var brandNames = batchLoadBrandNames(
                products.stream().map(ProductEntity::getBrandId).filter(Objects::nonNull).distinct().toList()
        );

        return products.stream()
                .map(e -> toListDataBatched(e, imagesMap, statsMap, reviewStatsMap, categoryNames, categorySlugs, brandNames))
                .toList();
    }

    private Map<Long, String> batchLoadCategorySlugs(List<Long> categoryIds) {
        if (categoryIds.isEmpty()) return Map.of();
        return categoryJpa.findAllById(categoryIds).stream()
                .collect(Collectors.toMap(
                        CategoryEntity::getId,
                        CategoryEntity::getSlug
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
            Map<Long, String> categorySlugs,
            Map<Long, String> brandNames
    ) {
        List<String> images = imagesMap.getOrDefault(e.getId(), List.of());
        ProductStats stats = statsMap.getOrDefault(e.getId(), new ProductStats(0, null, null));
        ReviewStats reviewStats = reviewStatsMap.getOrDefault(e.getId(), new ReviewStats(null, null));
        
        String badge = computeBadge(e, stats.variantCount);
        
        return new ProductData(
                e.getId(), e.getCategoryId(), categoryNames.get(e.getCategoryId()), categorySlugs.get(e.getCategoryId()),
                e.getBrandId(), brandNames.get(e.getBrandId()),
                e.getName(), e.getSlug(), e.getDescription(), e.getShortDescription(), e.getThumbnail(),
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
