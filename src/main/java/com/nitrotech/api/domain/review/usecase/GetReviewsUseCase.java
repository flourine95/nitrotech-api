package com.nitrotech.api.domain.review.usecase;

import com.nitrotech.api.domain.review.dto.ReviewData;
import com.nitrotech.api.domain.review.repository.ReviewRepository;
import com.nitrotech.api.shared.response.ApiResult;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class GetReviewsUseCase {

    private final ReviewRepository reviewRepository;

    public GetReviewsUseCase(ReviewRepository reviewRepository) {
        this.reviewRepository = reviewRepository;
    }

    public ApiResult<List<ReviewData>> executeByProduct(Long productId, String status, int page, int size) {
        List<ReviewData> data = reviewRepository.findByProductId(productId, status, page, size);
        long total = reviewRepository.countByProductId(productId, status);
        return ApiResult.paginated(data, page, size, total);
    }

    public ApiResult<List<ReviewData>> executePending(int page, int size) {
        List<ReviewData> data = reviewRepository.findPending(page, size);
        long total = reviewRepository.countPending();
        return ApiResult.paginated(data, page, size, total);
    }
}
