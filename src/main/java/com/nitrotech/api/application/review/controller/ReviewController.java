package com.nitrotech.api.application.review.controller;

import com.nitrotech.api.application.review.request.CreateReviewRequest;
import com.nitrotech.api.domain.auth.usecase.GetProfileUseCase;
import com.nitrotech.api.domain.review.dto.CreateReviewCommand;
import com.nitrotech.api.domain.review.dto.ReviewData;
import com.nitrotech.api.domain.review.usecase.*;
import com.nitrotech.api.shared.response.ApiResponse;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
public class ReviewController {

    private final CreateReviewUseCase createReviewUseCase;
    private final GetReviewsUseCase getReviewsUseCase;
    private final ModerateReviewUseCase moderateReviewUseCase;
    private final GetProfileUseCase getProfileUseCase;

    public ReviewController(CreateReviewUseCase createReviewUseCase, GetReviewsUseCase getReviewsUseCase,
                             ModerateReviewUseCase moderateReviewUseCase, GetProfileUseCase getProfileUseCase) {
        this.createReviewUseCase = createReviewUseCase;
        this.getReviewsUseCase = getReviewsUseCase;
        this.moderateReviewUseCase = moderateReviewUseCase;
        this.getProfileUseCase = getProfileUseCase;
    }

    // Public — lấy review đã approved của product
    @GetMapping("/api/products/{productId}/reviews")
    public ResponseEntity<ApiResponse<List<ReviewData>>> listByProduct(
            @PathVariable Long productId,
            @RequestParam(defaultValue = "approved") String status,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size
    ) {
        return ResponseEntity.ok(getReviewsUseCase.executeByProduct(productId, status, page, size));
    }

    // User — tạo review
    @PostMapping("/api/reviews")
    public ResponseEntity<ApiResponse<ReviewData>> create(
            @AuthenticationPrincipal String email,
            @Valid @RequestBody CreateReviewRequest req
    ) {
        ReviewData data = createReviewUseCase.execute(new CreateReviewCommand(
                userId(email), req.productId(), req.orderId(),
                req.rating(), req.comment(), req.images()));
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.created(data));
    }

    // Admin — danh sách review chờ duyệt
    @GetMapping("/api/admin/reviews/pending")
    public ResponseEntity<ApiResponse<List<ReviewData>>> pending(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size
    ) {
        return ResponseEntity.ok(getReviewsUseCase.executePending(page, size));
    }

    // Admin — duyệt review
    @PatchMapping("/api/admin/reviews/{id}/approve")
    public ResponseEntity<ApiResponse<ReviewData>> approve(@PathVariable Long id) {
        return ResponseEntity.ok(ApiResponse.ok(moderateReviewUseCase.approve(id)));
    }

    // Admin — từ chối review
    @PatchMapping("/api/admin/reviews/{id}/reject")
    public ResponseEntity<ApiResponse<ReviewData>> reject(@PathVariable Long id) {
        return ResponseEntity.ok(ApiResponse.ok(moderateReviewUseCase.reject(id)));
    }

    private Long userId(String email) {
        return getProfileUseCase.executeByEmail(email).id();
    }
}
