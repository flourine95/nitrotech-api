package com.nitrotech.api.domain.brand.usecase;

import com.nitrotech.api.domain.brand.dto.BulkResult;
import com.nitrotech.api.domain.brand.repository.BrandRepository;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;
import java.util.Set;

@Service
public class BulkHardDeleteBrandUseCase {

    private final BrandRepository brandRepository;
    private final ProductBrandChecker productBrandChecker;

    public BulkHardDeleteBrandUseCase(BrandRepository brandRepository,
                                       ProductBrandChecker productBrandChecker) {
        this.brandRepository = brandRepository;
        this.productBrandChecker = productBrandChecker;
    }

    public BulkResult execute(List<Long> ids) {
        // Check products trước
        Set<Long> hasProducts = productBrandChecker.filterHasProducts(ids);
        List<Long> eligible = ids.stream().filter(id -> !hasProducts.contains(id)).toList();

        // Thử hard delete eligible
        List<Long> deleted = brandRepository.bulkHardDelete(eligible);
        Set<Long> deletedSet = Set.copyOf(deleted);

        // Phân loại lý do fail
        Map<Long, String> failedReasons = new java.util.LinkedHashMap<>();
        for (Long id : ids) {
            if (deletedSet.contains(id)) {
                continue; // thành công, bỏ qua
            }
            if (hasProducts.contains(id)) {
                failedReasons.put(id, "Brand still has active products");
            } else {
                // eligible nhưng không delete được → không tồn tại hoặc chưa soft delete
                failedReasons.put(id, "Brand not found or not soft-deleted yet");
            }
        }

        List<Long> failed = List.copyOf(failedReasons.keySet());
        return new BulkResult(deleted.size(), failed.size(), failed, failedReasons);
    }
}
