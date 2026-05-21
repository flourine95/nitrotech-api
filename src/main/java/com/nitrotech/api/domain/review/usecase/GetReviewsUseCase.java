package com.nitrotech.api.domain.review.usecase;

import com.nitrotech.api.domain.review.dto.ReviewData;
import com.nitrotech.api.domain.review.repository.ReviewRepository;
import com.nitrotech.api.shared.response.ApiResult;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class GetReviewsUseCase {

    private final ReviewRepository reviewRepository;

    public GetReviewsUseCase(ReviewRepository reviewRepository) {
        this.reviewRepository = reviewRepository;
    }

    public ApiResult<List<ReviewData>> executeByProduct(Long productId, String status, int page, int size) {
        Page<ReviewData> result = reviewRepository.findByProductId(productId, status, PageRequest.of(page, size));
        return ApiResult.paged(result);
    }

    public ApiResult<List<ReviewData>> executePending(int page, int size) {
        Page<ReviewData> result = reviewRepository.findPending(PageRequest.of(page, size));
        return ApiResult.paged(result);
    }
}
