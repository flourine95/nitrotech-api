package com.nitrotech.api.infrastructure.persistence.repository;

import com.nitrotech.api.infrastructure.persistence.entity.BrandEntity;
import com.nitrotech.api.infrastructure.persistence.entity.CategoryEntity;
import com.nitrotech.api.infrastructure.persistence.entity.ProductEntity;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@Transactional
class SoftDeletedSlugRepositoryTest {

    @Autowired
    private BrandJpaRepository brandRepository;

    @Autowired
    private CategoryJpaRepository categoryRepository;

    @Autowired
    private ProductJpaRepository productRepository;

    @Test
    void brandSlugCheckIgnoresDeletedBrands() {
        brandRepository.save(brand("Deleted Brand", "shared-slug", Instant.now()));

        assertThat(brandRepository.existsNotDeletedBySlug("shared-slug")).isFalse();
    }

    @Test
    void brandSlugCheckDetectsActiveBrands() {
        brandRepository.save(brand("Active Brand", "shared-slug", null));

        assertThat(brandRepository.existsNotDeletedBySlug("shared-slug")).isTrue();
    }

    @Test
    void categorySlugCheckIgnoresDeletedCategories() {
        categoryRepository.save(category("Deleted Category", "shared-slug", Instant.now()));

        assertThat(categoryRepository.existsNotDeletedBySlug("shared-slug")).isFalse();
    }

    @Test
    void categorySlugCheckDetectsActiveCategories() {
        categoryRepository.save(category("Active Category", "shared-slug", null));

        assertThat(categoryRepository.existsNotDeletedBySlug("shared-slug")).isTrue();
    }

    @Test
    void productSlugCheckIgnoresDeletedProducts() {
        CategoryEntity category = categoryRepository.save(category("Product Category", "product-category", null));
        productRepository.save(product(category.getId(), "Deleted Product", "shared-slug", Instant.now()));

        assertThat(productRepository.existsNotDeletedBySlug("shared-slug")).isFalse();
    }

    @Test
    void productSlugCheckDetectsActiveProducts() {
        CategoryEntity category = categoryRepository.save(category("Product Category", "product-category", null));
        productRepository.save(product(category.getId(), "Active Product", "shared-slug", null));

        assertThat(productRepository.existsNotDeletedBySlug("shared-slug")).isTrue();
    }

    private BrandEntity brand(String name, String slug, Instant deletedAt) {
        BrandEntity brand = new BrandEntity();
        brand.setName(name);
        brand.setSlug(slug);
        brand.setDeletedAt(deletedAt);
        return brand;
    }

    private CategoryEntity category(String name, String slug, Instant deletedAt) {
        CategoryEntity category = new CategoryEntity();
        category.setName(name);
        category.setSlug(slug);
        category.setDeletedAt(deletedAt);
        return category;
    }

    private ProductEntity product(Long categoryId, String name, String slug, Instant deletedAt) {
        ProductEntity product = new ProductEntity();
        product.setCategoryId(categoryId);
        product.setName(name);
        product.setSlug(slug);
        product.setDeletedAt(deletedAt);
        return product;
    }
}
