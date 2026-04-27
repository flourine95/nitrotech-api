package com.nitrotech.api.domain.brand.usecase;

import com.nitrotech.api.domain.brand.dto.BulkResult;
import com.nitrotech.api.domain.brand.repository.BrandRepository;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Set;

@Service
public class BulkDeleteBrandUseCase {

    private final BrandRepository brandRepository;

    public BulkDeleteBrandUseCase(BrandRepository brandRepository) {
        this.brandRepository = brandRepository;
    }

    public BulkResult execute(List<Long> ids) {
        List<Long> deleted = brandRepository.bulkSoftDelete(ids);
        Set<Long> deletedSet = Set.copyOf(deleted);
        List<Long> failed = ids.stream().filter(id -> !deletedSet.contains(id)).toList();
        return new BulkResult(deleted.size(), failed.size(), failed);
    }
}
