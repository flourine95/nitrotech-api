package com.nitrotech.api.application.banner.controller;

import com.nitrotech.api.application.banner.request.CreateBannerRequest;
import com.nitrotech.api.application.banner.request.UpdateBannerRequest;
import com.nitrotech.api.domain.banner.dto.BannerData;
import com.nitrotech.api.domain.banner.dto.CreateBannerCommand;
import com.nitrotech.api.domain.banner.dto.UpdateBannerCommand;
import com.nitrotech.api.domain.banner.usecase.*;
import com.nitrotech.api.shared.response.ApiResponse;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/banners")
public class BannerController {

    private final GetBannersUseCase getBannersUseCase;
    private final CreateBannerUseCase createBannerUseCase;
    private final UpdateBannerUseCase updateBannerUseCase;
    private final DeleteBannerUseCase deleteBannerUseCase;

    public BannerController(GetBannersUseCase getBannersUseCase, CreateBannerUseCase createBannerUseCase,
                             UpdateBannerUseCase updateBannerUseCase, DeleteBannerUseCase deleteBannerUseCase) {
        this.getBannersUseCase = getBannersUseCase;
        this.createBannerUseCase = createBannerUseCase;
        this.updateBannerUseCase = updateBannerUseCase;
        this.deleteBannerUseCase = deleteBannerUseCase;
    }

    // Public — chỉ trả banner active trong date range
    @GetMapping
    public ResponseEntity<ApiResponse<List<BannerData>>> list(
            @RequestParam(required = false) String position
    ) {
        return ResponseEntity.ok(ApiResponse.ok(getBannersUseCase.executeActive(position)));
    }

    // Admin — lấy tất cả với filter
    @GetMapping("/admin")
    public ResponseEntity<ApiResponse<List<BannerData>>> listAll(
            @RequestParam(required = false) Boolean active,
            @RequestParam(required = false) String position
    ) {
        return ResponseEntity.ok(ApiResponse.ok(getBannersUseCase.executeAll(active, position)));
    }

    @PostMapping
    public ResponseEntity<ApiResponse<BannerData>> create(@Valid @RequestBody CreateBannerRequest req) {
        BannerData data = createBannerUseCase.execute(new CreateBannerCommand(
                req.title(), req.image(), req.url(), req.position(),
                req.active(), req.startDate(), req.endDate(), req.sortOrder()));
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.created(data));
    }

    @PutMapping("/{id}")
    public ResponseEntity<ApiResponse<BannerData>> update(
            @PathVariable Long id, @Valid @RequestBody UpdateBannerRequest req) {
        BannerData data = updateBannerUseCase.execute(new UpdateBannerCommand(
                id, req.title(), req.image(), req.url(), req.position(),
                req.active(), req.startDate(), req.endDate(), req.sortOrder()));
        return ResponseEntity.ok(ApiResponse.ok(data));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<ApiResponse<Void>> delete(@PathVariable Long id) {
        deleteBannerUseCase.execute(id);
        return ResponseEntity.ok(ApiResponse.ok(null, "Banner deleted successfully"));
    }
}
