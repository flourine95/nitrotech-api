package com.nitrotech.api.domain.product.dto;

import java.util.List;
import java.util.Map;

public record CreateProductCommand(
        Long categoryId,
        Long brandId,
        String name,
        String slug,
        String description,
        String thumbnail,
        Map<String, Object> specs,
        boolean active,
        List<String> images,
        List<CreateVariantCommand> variants
) {}
