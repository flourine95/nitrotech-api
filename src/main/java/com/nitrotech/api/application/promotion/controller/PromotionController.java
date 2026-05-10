package com.nitrotech.api.application.promotion.controller;

import com.nitrotech.api.application.promotion.request.CreatePromotionRequest;
import com.nitrotech.api.domain.promotion.dto.ApplyPromotionResult;
import com.nitrotech.api.domain.promotion.dto.CreatePromotionCommand;
import com.nitrotech.api.domain.promotion.dto.PromotionData;
import com.nitrotech.api.domain.promotion.usecase.ManagePromotionUseCase;
import com.nitrotech.api.domain.promotion.usecase.ValidatePromotionUseCase;
import com.nitrotech.api.shared.response.ApiResult;
import com.nitrotech.api.shared.security.UserPrincipal;
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

import java.math.BigDecimal;
import java.util.List;

@RestController
@RequestMapping("/api")
@Tag(name = "Promotions", description = "Promotion code validation and admin management APIs")
@RequiredArgsConstructor
public class PromotionController {

    private final ManagePromotionUseCase managePromotionUseCase;
    private final ValidatePromotionUseCase validatePromotionUseCase;

    @Operation(summary = "Validate promotion code", description = "Check if a promotion code is valid for the current user and order amount. Use before checkout to preview the discount.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Promotion is valid, discount details returned"),
            @ApiResponse(responseCode = "400", description = "Promotion code is invalid, expired, or usage limit reached", content = @Content(mediaType = "application/json")),
            @ApiResponse(responseCode = "401", description = "Not authenticated", content = @Content(mediaType = "application/json"))
    })
    @GetMapping("/promotions/validate")
    public ResponseEntity<ApiResult<ApplyPromotionResult>> validate(
            @AuthenticationPrincipal UserPrincipal principal,
            @Parameter(description = "Promotion code to validate") @RequestParam String code,
            @Parameter(description = "Order amount to calculate discount against") @RequestParam BigDecimal orderAmount
    ) {
        return ResponseEntity.ok(ApiResult.ok(
                validatePromotionUseCase.execute(code, principal.id(), orderAmount)));
    }

    @Operation(summary = "List promotions (admin)", description = "Admin endpoint. Get paginated list of promotions with optional status filter.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Promotions retrieved successfully"),
            @ApiResponse(responseCode = "401", description = "Not authenticated", content = @Content(mediaType = "application/json")),
            @ApiResponse(responseCode = "403", description = "Insufficient permissions", content = @Content(mediaType = "application/json"))
    })
    @GetMapping("/admin/promotions")
    public ResponseEntity<ApiResult<List<PromotionData>>> list(
            @Parameter(description = "Filter by status (active, inactive, expired)") @RequestParam(required = false) String status,
            @Parameter(description = "Page number (0-based)") @RequestParam(defaultValue = "0") int page,
            @Parameter(description = "Page size") @RequestParam(defaultValue = "20") int size
    ) {
        List<PromotionData> data = managePromotionUseCase.findAll(status, page, size);
        long total = managePromotionUseCase.countAll(status);
        return ResponseEntity.ok(ApiResult.paginated(data, page, size, total));
    }

    @Operation(summary = "Get promotion by ID (admin)", description = "Admin endpoint. Retrieve a single promotion by its ID.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Promotion found"),
            @ApiResponse(responseCode = "401", description = "Not authenticated", content = @Content(mediaType = "application/json")),
            @ApiResponse(responseCode = "403", description = "Insufficient permissions", content = @Content(mediaType = "application/json")),
            @ApiResponse(responseCode = "404", description = "Promotion not found", content = @Content(mediaType = "application/json"))
    })
    @GetMapping("/admin/promotions/{id}")
    public ResponseEntity<ApiResult<PromotionData>> get(
            @Parameter(description = "Promotion ID") @PathVariable Long id
    ) {
        return ResponseEntity.ok(ApiResult.ok(managePromotionUseCase.findById(id)));
    }

    @Operation(summary = "Create promotion (admin)", description = "Admin endpoint. Create a new promotion code with discount rules and scheduling.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "201", description = "Promotion created successfully"),
            @ApiResponse(responseCode = "400", description = "Invalid input - validation error", content = @Content(mediaType = "application/json")),
            @ApiResponse(responseCode = "401", description = "Not authenticated", content = @Content(mediaType = "application/json")),
            @ApiResponse(responseCode = "403", description = "Insufficient permissions", content = @Content(mediaType = "application/json")),
            @ApiResponse(responseCode = "409", description = "Promotion code already exists", content = @Content(mediaType = "application/json"))
    })
    @PostMapping("/admin/promotions")
    public ResponseEntity<ApiResult<PromotionData>> create(@Valid @RequestBody CreatePromotionRequest req) {
        PromotionData data = managePromotionUseCase.create(toCommand(req));
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResult.created(data));
    }

    @Operation(summary = "Update promotion (admin)", description = "Admin endpoint. Update an existing promotion's rules and scheduling.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Promotion updated successfully"),
            @ApiResponse(responseCode = "400", description = "Invalid input - validation error", content = @Content(mediaType = "application/json")),
            @ApiResponse(responseCode = "401", description = "Not authenticated", content = @Content(mediaType = "application/json")),
            @ApiResponse(responseCode = "403", description = "Insufficient permissions", content = @Content(mediaType = "application/json")),
            @ApiResponse(responseCode = "404", description = "Promotion not found", content = @Content(mediaType = "application/json"))
    })
    @PutMapping("/admin/promotions/{id}")
    public ResponseEntity<ApiResult<PromotionData>> update(
            @Parameter(description = "Promotion ID") @PathVariable Long id,
            @Valid @RequestBody CreatePromotionRequest req
    ) {
        return ResponseEntity.ok(ApiResult.ok(managePromotionUseCase.update(id, toCommand(req))));
    }

    @Operation(summary = "Update promotion status (admin)", description = "Admin endpoint. Change the status of a promotion (e.g. active, inactive).")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Promotion status updated"),
            @ApiResponse(responseCode = "400", description = "Invalid status value", content = @Content(mediaType = "application/json")),
            @ApiResponse(responseCode = "401", description = "Not authenticated", content = @Content(mediaType = "application/json")),
            @ApiResponse(responseCode = "403", description = "Insufficient permissions", content = @Content(mediaType = "application/json")),
            @ApiResponse(responseCode = "404", description = "Promotion not found", content = @Content(mediaType = "application/json"))
    })
    @PatchMapping("/admin/promotions/{id}/status")
    public ResponseEntity<ApiResult<PromotionData>> updateStatus(
            @Parameter(description = "Promotion ID") @PathVariable Long id,
            @Parameter(description = "New status value") @RequestParam String status
    ) {
        return ResponseEntity.ok(ApiResult.ok(managePromotionUseCase.updateStatus(id, status)));
    }

    @Operation(summary = "Delete promotion (admin)", description = "Admin endpoint. Permanently remove a promotion.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Promotion deleted"),
            @ApiResponse(responseCode = "401", description = "Not authenticated", content = @Content(mediaType = "application/json")),
            @ApiResponse(responseCode = "403", description = "Insufficient permissions", content = @Content(mediaType = "application/json")),
            @ApiResponse(responseCode = "404", description = "Promotion not found", content = @Content(mediaType = "application/json"))
    })
    @DeleteMapping("/admin/promotions/{id}")
    public ResponseEntity<ApiResult<Void>> delete(
            @Parameter(description = "Promotion ID") @PathVariable Long id
    ) {
        managePromotionUseCase.delete(id);
        return ResponseEntity.ok(ApiResult.ok(null, "Promotion deleted"));
    }

    private CreatePromotionCommand toCommand(CreatePromotionRequest req) {
        return new CreatePromotionCommand(req.name(), req.description(), req.code(), req.type(),
                req.discountValue(), req.minOrderAmount(), req.maxDiscountAmount(),
                req.stackable(), req.priority(), req.usageLimit(), req.usagePerUser(),
                req.startAt(), req.endAt(), req.status());
    }
}
