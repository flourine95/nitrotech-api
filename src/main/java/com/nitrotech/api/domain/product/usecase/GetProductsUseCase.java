package com.nitrotech.api.domain.product.usecase;

import com.nitrotech.api.domain.product.dto.ProductData;
import com.nitrotech.api.domain.product.dto.ProductListQuery;
import com.nitrotech.api.domain.product.repository.ProductRepository;
import com.nitrotech.api.shared.response.ApiResponse;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class GetProductsUseCase {

    private final ProductRepository productRepository;

    public GetProductsUseCase(ProductRepository productRepository) {
        this.productRepository = productRepository;
    }

    public ApiResponse<List<ProductData>> execute(ProductListQuery query) {
        List<ProductData> data = productRepository.findAll(query);
        long total = productRepository.countAll(query);
        return ApiResponse.paginated(data, query.page(), query.size(), total);
    }
}
