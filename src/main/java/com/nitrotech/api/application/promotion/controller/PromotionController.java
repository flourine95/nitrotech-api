package com.nitrotech.api.application.promotion.controller;

import com.nitrotech.api.application.promotion.request.CreatePromotionRequest;
import com.nitrotech.api.domain.auth.usecase.GetProfileUseCase;
import com.nitrotech.api.domain.promotion.dto.ApplyPromotionResult;
import com.nitrotech.api.domain.promotion.dto.CreatePromotionCommand;
import com.nitrotech.api.domain.promotion.dto.PromotionData;
import com.nitrotech.api.domain.promotion.usecase.ManagePromotionUseCase;
import com.nitrotech.api.domain.promotion.usecase.ValidatePromotionUseCase;
import com.nitrotech.api.shared.response.ApiResponse;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.List;

@RestController
@RequestMapping("/api")
public class PromotionController {

    private final ManagePromotionUseCase managePromotionUseCase;
    private final ValidatePromotionUseCase validatePromotionUseCase;
    private final GetProfileUseCase getProfileUseCase;

    public PromotionController(ManagePromotionUseCase managePromotionUseCase,
                                ValidatePromotionUseCase validatePromotionUseCase,
                                GetProfileUseCase getProfileUseCase) {
        this.managePromotionUseCase = managePromotionUseCase;
        this.validatePromotionUseCase = validatePromotionUseCase;
        this.getProfileUseCase = getProfileUseCase;
    }

    // User — validate promotion code trước khi checkout
    @GetMapping("/promotions/validate")
    public ResponseEntity<ApiResponse<ApplyPromotionResult>> validate(
            @AuthenticationPrincipal String email,
            @RequestParam String code,
            @RequestParam BigDecimal orderAmount
    ) {
        Long userId = getProfileUseCase.executeByEmail(email).id();
        return ResponseEntity.ok(ApiResponse.ok(
                validatePromotionUseCase.execute(code, userId, orderAmount)));
    }

    // Admin
    @GetMapping("/admin/promotions")
    public ResponseEntity<ApiResponse<List<PromotionData>>> list(
            @RequestParam(required = false) String status,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size
    ) {
        List<PromotionData> data = managePromotionUseCase.findAll(status, page, size);
        long total = managePromotionUseCase.countAll(status);
        return ResponseEntity.ok(ApiResponse.paginated(data, page, size, total));
    }

    @GetMapping("/admin/promotions/{id}")
    public ResponseEntity<ApiResponse<PromotionData>> get(@PathVariable Long id) {
        return ResponseEntity.ok(ApiResponse.ok(managePromotionUseCase.findById(id)));
    }

    @PostMapping("/admin/promotions")
    public ResponseEntity<ApiResponse<PromotionData>> create(@Valid @RequestBody CreatePromotionRequest req) {
        PromotionData data = managePromotionUseCase.create(toCommand(req));
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.created(data));
    }

    @PutMapping("/admin/promotions/{id}")
    public ResponseEntity<ApiResponse<PromotionData>> update(
            @PathVariable Long id, @Valid @RequestBody CreatePromotionRequest req) {
        return ResponseEntity.ok(ApiResponse.ok(managePromotionUseCase.update(id, toCommand(req))));
    }

    @PatchMapping("/admin/promotions/{id}/status")
    public ResponseEntity<ApiResponse<PromotionData>> updateStatus(
            @PathVariable Long id, @RequestParam String status) {
        return ResponseEntity.ok(ApiResponse.ok(managePromotionUseCase.updateStatus(id, status)));
    }

    @DeleteMapping("/admin/promotions/{id}")
    public ResponseEntity<ApiResponse<Void>> delete(@PathVariable Long id) {
        managePromotionUseCase.delete(id);
        return ResponseEntity.ok(ApiResponse.ok(null, "Promotion deleted"));
    }

    private CreatePromotionCommand toCommand(CreatePromotionRequest req) {
        return new CreatePromotionCommand(req.name(), req.description(), req.code(), req.type(),
                req.discountValue(), req.minOrderAmount(), req.maxDiscountAmount(),
                req.stackable(), req.priority(), req.usageLimit(), req.usagePerUser(),
                req.startAt(), req.endAt(), req.status());
    }
}
