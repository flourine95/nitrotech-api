package com.nitrotech.api.domain.brand.repository;

import com.nitrotech.api.domain.brand.dto.BrandData;
import com.nitrotech.api.domain.brand.dto.BrandFacets;
import com.nitrotech.api.domain.brand.dto.BrandFilter;
import com.nitrotech.api.domain.brand.dto.CreateBrandCommand;
import com.nitrotech.api.domain.brand.dto.UpdateBrandCommand;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.List;
import java.util.Optional;

public interface BrandRepository {
    BrandData create(CreateBrandCommand command);
    BrandData update(UpdateBrandCommand command);
    Optional<BrandData> findById(Long id);
    Optional<BrandData> findBySlug(String slug);
    Optional<BrandData> findDeletedById(Long id);
    Page<BrandData> findAll(BrandFilter filter, Pageable pageable);
    BrandFacets countFacets(String search);
    boolean existsById(Long id);
    boolean existsBySlug(String slug);
    boolean existsBySlugAndIdNot(String slug, Long id);
    void softDelete(Long id);
    void restore(Long id);
    void hardDelete(Long id);
    List<Long> bulkSoftDelete(List<Long> ids);
    List<Long> bulkRestore(List<Long> ids);
    List<Long> bulkHardDelete(List<Long> ids);
}
