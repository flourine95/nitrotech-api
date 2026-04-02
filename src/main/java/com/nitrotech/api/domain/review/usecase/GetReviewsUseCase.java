package com.nitrotech.api.domain.review.usecase;

import com.nitrotech.api.domain.review.dto.ReviewData;
import com.nitrotech.api.domain.review.repository.ReviewRepository;
import com.nitrotech.api.shared.response.ApiResponse;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class GetReviewsUseCase {

    private final ReviewRepository reviewRepository;

    public GetReviewsUseCase(ReviewRepository reviewRepository) {
        this.reviewRepository = reviewRepository;
    }

    public ApiResponse<List<ReviewData>> executeByProduct(Long productId, String status, int page, int size) {
        List<ReviewData> data = reviewRepository.findByProductId(productId, status, page, size);
        long total = reviewRepository.countByProductId(productId, status);
        return ApiResponse.paginated(data, page, size, total);
    }

    public ApiResponse<List<ReviewData>> executePending(int page, int size) {
        List<ReviewData> data = reviewRepository.findPending(page, size);
        long total = reviewRepository.countPending();
        return ApiResponse.paginated(data, page, size, total);
    }
}
