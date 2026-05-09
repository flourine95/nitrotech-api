package com.nitrotech.api.application.review.controller;

import com.nitrotech.api.application.review.request.CreateReviewRequest;
import com.nitrotech.api.domain.auth.usecase.GetProfileUseCase;
import com.nitrotech.api.domain.review.dto.CreateReviewCommand;
import com.nitrotech.api.domain.review.dto.ReviewData;
import com.nitrotech.api.domain.review.usecase.*;
import com.nitrotech.api.shared.response.ApiResult;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@Tag(name = "Reviews", description = "Product review and moderation APIs")
@RequiredArgsConstructor
public class ReviewController {

    private final CreateReviewUseCase createReviewUseCase;
    private final GetReviewsUseCase getReviewsUseCase;
    private final ModerateReviewUseCase moderateReviewUseCase;
    private final GetProfileUseCase getProfileUseCase;

    @Operation(summary = "List reviews for product", description = "Public endpoint. Get approved reviews for a product, paginated.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Reviews retrieved successfully"),
            @ApiResponse(responseCode = "404", description = "Product not found", content = @Content(mediaType = "application/json"))
    })
    @GetMapping("/api/products/{productId}/reviews")
    public ResponseEntity<ApiResult<List<ReviewData>>> listByProduct(
            @Parameter(description = "Product ID") @PathVariable Long productId,
            @Parameter(description = "Filter by status (default: approved)") @RequestParam(defaultValue = "approved") String status,
            @Parameter(description = "Page number (0-based)") @RequestParam(defaultValue = "0") int page,
            @Parameter(description = "Page size") @RequestParam(defaultValue = "10") int size
    ) {
        return ResponseEntity.ok(getReviewsUseCase.executeByProduct(productId, status, page, size));
    }

    @Operation(summary = "Create review", description = "Submit a review for a product. Requires a completed order containing the product.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "201", description = "Review submitted successfully, pending moderation"),
            @ApiResponse(responseCode = "400", description = "Invalid input - validation error", content = @Content(mediaType = "application/json")),
            @ApiResponse(responseCode = "401", description = "Not authenticated", content = @Content(mediaType = "application/json")),
            @ApiResponse(responseCode = "409", description = "Review already submitted for this product/order", content = @Content(mediaType = "application/json"))
    })
    @PostMapping("/api/reviews")
    public ResponseEntity<ApiResult<ReviewData>> create(
            @AuthenticationPrincipal String email,
            @Valid @RequestBody CreateReviewRequest req
    ) {
        ReviewData data = createReviewUseCase.execute(new CreateReviewCommand(
                userId(email), req.productId(), req.orderId(),
                req.rating(), req.comment(), req.images()));
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResult.created(data));
    }

    @Operation(summary = "List pending reviews (admin)", description = "Admin endpoint. Get reviews awaiting moderation, paginated.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Pending reviews retrieved"),
            @ApiResponse(responseCode = "401", description = "Not authenticated", content = @Content(mediaType = "application/json")),
            @ApiResponse(responseCode = "403", description = "Insufficient permissions", content = @Content(mediaType = "application/json"))
    })
    @GetMapping("/api/admin/reviews/pending")
    public ResponseEntity<ApiResult<List<ReviewData>>> pending(
            @Parameter(description = "Page number (0-based)") @RequestParam(defaultValue = "0") int page,
            @Parameter(description = "Page size") @RequestParam(defaultValue = "20") int size
    ) {
        return ResponseEntity.ok(getReviewsUseCase.executePending(page, size));
    }

    @Operation(summary = "Approve review (admin)", description = "Admin endpoint. Approve a pending review, making it publicly visible.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Review approved"),
            @ApiResponse(responseCode = "401", description = "Not authenticated", content = @Content(mediaType = "application/json")),
            @ApiResponse(responseCode = "403", description = "Insufficient permissions", content = @Content(mediaType = "application/json")),
            @ApiResponse(responseCode = "404", description = "Review not found", content = @Content(mediaType = "application/json"))
    })
    @PatchMapping("/api/admin/reviews/{id}/approve")
    public ResponseEntity<ApiResult<ReviewData>> approve(
            @Parameter(description = "Review ID") @PathVariable Long id
    ) {
        return ResponseEntity.ok(ApiResult.ok(moderateReviewUseCase.approve(id)));
    }

    @Operation(summary = "Reject review (admin)", description = "Admin endpoint. Reject a pending review, hiding it from public view.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Review rejected"),
            @ApiResponse(responseCode = "401", description = "Not authenticated", content = @Content(mediaType = "application/json")),
            @ApiResponse(responseCode = "403", description = "Insufficient permissions", content = @Content(mediaType = "application/json")),
            @ApiResponse(responseCode = "404", description = "Review not found", content = @Content(mediaType = "application/json"))
    })
    @PatchMapping("/api/admin/reviews/{id}/reject")
    public ResponseEntity<ApiResult<ReviewData>> reject(
            @Parameter(description = "Review ID") @PathVariable Long id
    ) {
        return ResponseEntity.ok(ApiResult.ok(moderateReviewUseCase.reject(id)));
    }

    private Long userId(String email) {
        return getProfileUseCase.executeByEmail(email).id();
    }
}
