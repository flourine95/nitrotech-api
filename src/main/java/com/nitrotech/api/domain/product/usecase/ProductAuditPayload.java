package com.nitrotech.api.domain.product.usecase;

import com.nitrotech.api.domain.product.dto.ProductData;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;

final class ProductAuditPayload {

    private ProductAuditPayload() {
    }

    static Map<String, Object> snapshot(ProductData product) {
        Map<String, Object> data = new LinkedHashMap<>();
        put(data, "name", product.name());
        put(data, "slug", product.slug());
        put(data, "categoryId", product.categoryId());
        put(data, "categoryName", product.categoryName());
        put(data, "brandId", product.brandId());
        put(data, "brandName", product.brandName());
        put(data, "shortDescription", product.shortDescription());
        put(data, "description", product.description());
        put(data, "thumbnail", product.thumbnail());
        put(data, "specs", product.specs());
        put(data, "active", product.active());
        put(data, "images", product.images());
        put(data, "variantCount", product.variantCount());
        put(data, "priceMin", product.priceMin());
        put(data, "priceMax", product.priceMax());
        put(data, "badge", product.badge());
        return data;
    }

    static ProductDelta delta(ProductData before, ProductData after) {
        Map<String, Object> beforeData = new LinkedHashMap<>();
        Map<String, Object> afterData = new LinkedHashMap<>();

        addIfChanged(beforeData, afterData, "name", before.name(), after.name());
        addIfChanged(beforeData, afterData, "slug", before.slug(), after.slug());
        addIfChanged(beforeData, afterData, "categoryId", before.categoryId(), after.categoryId());
        addIfChanged(beforeData, afterData, "categoryName", before.categoryName(), after.categoryName());
        addIfChanged(beforeData, afterData, "brandId", before.brandId(), after.brandId());
        addIfChanged(beforeData, afterData, "brandName", before.brandName(), after.brandName());
        addIfChanged(beforeData, afterData, "shortDescription", before.shortDescription(), after.shortDescription());
        addIfChanged(beforeData, afterData, "description", before.description(), after.description());
        addIfChanged(beforeData, afterData, "thumbnail", before.thumbnail(), after.thumbnail());
        addIfChanged(beforeData, afterData, "specs", before.specs(), after.specs());
        addIfChanged(beforeData, afterData, "active", before.active(), after.active());
        addIfChanged(beforeData, afterData, "images", before.images(), after.images());
        addIfChanged(beforeData, afterData, "variantCount", before.variantCount(), after.variantCount());
        addIfChanged(beforeData, afterData, "priceMin", before.priceMin(), after.priceMin());
        addIfChanged(beforeData, afterData, "priceMax", before.priceMax(), after.priceMax());
        addIfChanged(beforeData, afterData, "badge", before.badge(), after.badge());

        return new ProductDelta(beforeData, afterData);
    }

    private static void addIfChanged(
            Map<String, Object> beforeData,
            Map<String, Object> afterData,
            String key,
            Object before,
            Object after
    ) {
        if (!Objects.equals(before, after)) {
            put(beforeData, key, before);
            put(afterData, key, after);
        }
    }

    private static void put(Map<String, Object> data, String key, Object value) {
        if (value != null) {
            data.put(key, value);
        }
    }

    record ProductDelta(Map<String, Object> beforeData, Map<String, Object> afterData) {
    }
}
