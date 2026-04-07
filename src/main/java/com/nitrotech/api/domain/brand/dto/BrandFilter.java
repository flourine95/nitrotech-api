package com.nitrotech.api.domain.brand.dto;

public record BrandFilter(
        String search,   // tìm theo name hoặc slug
        Boolean active,  // null = tất cả
        Boolean deleted  // null = tất cả, true = chỉ deleted, false = chưa deleted
) {}
