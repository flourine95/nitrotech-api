package com.nitrotech.api.application.promotion.controller;

import com.nitrotech.api.application.promotion.request.CreatePromotionRequest;
import com.nitrotech.api.domain.promotion.dto.ApplyPromotionResult;
import com.nitrotech.api.domain.promotion.dto.CreatePromotionCommand;
import com.nitrotech.api.domain.promotion.dto.PromotionData;
import com.nitrotech.api.domain.promotion.usecase.ManagePromotionUseCase;
import com.nitrotech.api.domain.promotion.usecase.ValidatePromotionUseCase;
import com.nitrotech.api.shared.response.ApiResult;
import com.nitrotech.api.shared.security.UserPrincipal;
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
@RequiredArgsConstructor
public class PromotionController {

    private final ManagePromotionUseCase managePromotionUseCase;
    private final ValidatePromotionUseCase validatePromotionUseCase;

    @GetMapping("/promotions/validate")
    public ResponseEntity<ApiResult<ApplyPromotionResult>> validate(
            @AuthenticationPrincipal UserPrincipal principal,
            @RequestParam String code,
            @RequestParam BigDecimal orderAmount
    ) {
        return ResponseEntity.ok(ApiResult.ok(
                validatePromotionUseCase.execute(code, principal.id(), orderAmount)));
    }

    @GetMapping("/admin/promotions")
    public ResponseEntity<ApiResult<List<PromotionData>>> list(
            @RequestParam(required = false) String status,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size
    ) {
        List<PromotionData> data = managePromotionUseCase.findAll(status, page, size);
        long total = managePromotionUseCase.countAll(status);
        return ResponseEntity.ok(ApiResult.paginated(data, page, size, total));
    }

    @GetMapping("/admin/promotions/{id}")
    public ResponseEntity<ApiResult<PromotionData>> get(
            @PathVariable Long id
    ) {
        return ResponseEntity.ok(ApiResult.ok(managePromotionUseCase.findById(id)));
    }

    @PostMapping("/admin/promotions")
    public ResponseEntity<ApiResult<PromotionData>> create(@Valid @RequestBody CreatePromotionRequest req) {
        PromotionData data = managePromotionUseCase.create(toCommand(req));
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResult.created(data));
    }

    @PutMapping("/admin/promotions/{id}")
    public ResponseEntity<ApiResult<PromotionData>> update(
            @PathVariable Long id,
            @Valid @RequestBody CreatePromotionRequest req
    ) {
        return ResponseEntity.ok(ApiResult.ok(managePromotionUseCase.update(id, toCommand(req))));
    }

    @PatchMapping("/admin/promotions/{id}/status")
    public ResponseEntity<ApiResult<PromotionData>> updateStatus(
            @PathVariable Long id,
            @RequestParam String status
    ) {
        return ResponseEntity.ok(ApiResult.ok(managePromotionUseCase.updateStatus(id, status)));
    }

    @DeleteMapping("/admin/promotions/{id}")
    public ResponseEntity<ApiResult<Void>> delete(
            @PathVariable Long id
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