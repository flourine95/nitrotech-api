package com.nitrotech.api.domain.brand.repository;

import com.nitrotech.api.domain.brand.dto.BrandData;
import com.nitrotech.api.domain.brand.dto.CreateBrandCommand;
import com.nitrotech.api.domain.brand.dto.UpdateBrandCommand;

import java.util.List;
import java.util.Optional;

public interface BrandRepository {
    BrandData create(CreateBrandCommand command);
    BrandData update(UpdateBrandCommand command);
    Optional<BrandData> findById(Long id);
    List<BrandData> findAll(Boolean active);
    boolean existsById(Long id);
    boolean existsBySlug(String slug);
    boolean existsBySlugAndIdNot(String slug, Long id);
    void softDelete(Long id);
}
