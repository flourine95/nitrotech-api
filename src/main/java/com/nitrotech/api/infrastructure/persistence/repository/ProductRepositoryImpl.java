package com.nitrotech.api.infrastructure.persistence.repository;

import com.nitrotech.api.domain.product.dto.*;
import com.nitrotech.api.domain.product.repository.ProductRepository;
import com.nitrotech.api.infrastructure.persistence.entity.*;
import com.nitrotech.api.shared.exception.NotFoundException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
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

    public ProductRepositoryImpl(ProductJpaRepository productJpa,
                                  ProductImageJpaRepository imageJpa,
                                  ProductVariantJpaRepository variantJpa) {
        this.productJpa = productJpa;
        this.imageJpa = imageJpa;
        this.variantJpa = variantJpa;
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
        ProductEntity saved = productJpa.save(entity);

        saveImages(saved.getId(), command.images());

        if (command.variants() != null) {
            command.variants().forEach(v -> saveVariant(saved.getId(), v));
        }

        return toData(saved);
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
        entity.setUpdatedAt(LocalDateTime.now());
        ProductEntity saved = productJpa.save(entity);

        if (command.images() != null) {
            imageJpa.deleteByProductId(saved.getId());
            saveImages(saved.getId(), command.images());
        }

        return toData(saved);
    }

    @Override
    public Optional<ProductData> findById(Long id) {
        return productJpa.findActiveById(id).map(this::toData);
    }

    @Override
    public List<ProductData> findAll(ProductListQuery query) {
        Page<ProductEntity> page = productJpa.findAllFiltered(
                query.categoryId(), query.brandId(), query.active(), query.search(),
                PageRequest.of(query.page(), query.size())
        );
        return page.getContent().stream().map(this::toData).toList();
    }

    @Override
    public long countAll(ProductListQuery query) {
        return productJpa.findAllFiltered(
                query.categoryId(), query.brandId(), query.active(), query.search(),
                PageRequest.of(query.page(), query.size())
        ).getTotalElements();
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
        return variantJpa.save(entity);
    }

    private ProductData toData(ProductEntity e) {
        List<String> images = imageJpa.findByProductIdOrderBySortOrderAsc(e.getId())
                .stream().map(ProductImageEntity::getUrl).toList();
        List<ProductVariantData> variants = variantJpa.findActiveByProductId(e.getId())
                .stream().map(this::toVariantData).toList();
        return new ProductData(
                e.getId(), e.getCategoryId(), null, e.getBrandId(), null,
                e.getName(), e.getSlug(), e.getDescription(), e.getThumbnail(),
                e.getSpecs(), e.isActive(), images, variants,
                e.getCreatedAt(), e.getUpdatedAt()
        );
    }

    private ProductVariantData toVariantData(ProductVariantEntity e) {
        return new ProductVariantData(
                e.getId(), e.getProductId(), e.getSku(), e.getName(),
                e.getPrice(), e.getAttributes(), e.isActive(),
                e.getCreatedAt(), e.getUpdatedAt()
        );
    }
}
