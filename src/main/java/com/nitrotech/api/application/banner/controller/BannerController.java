package com.nitrotech.api.application.banner.controller;

import com.nitrotech.api.application.banner.request.CreateBannerRequest;
import com.nitrotech.api.application.banner.request.UpdateBannerRequest;
import com.nitrotech.api.domain.banner.dto.BannerData;
import com.nitrotech.api.domain.banner.dto.CreateBannerCommand;
import com.nitrotech.api.domain.banner.dto.UpdateBannerCommand;
import com.nitrotech.api.domain.banner.usecase.*;
import com.nitrotech.api.shared.response.ApiResult;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/banners")
@RequiredArgsConstructor
public class BannerController {

    private final GetBannersUseCase getBannersUseCase;
    private final CreateBannerUseCase createBannerUseCase;
    private final UpdateBannerUseCase updateBannerUseCase;
    private final DeleteBannerUseCase deleteBannerUseCase;

    @GetMapping
    public ResponseEntity<ApiResult<List<BannerData>>> list(@RequestParam(required = false) String position) {
        return ResponseEntity.ok(ApiResult.ok(getBannersUseCase.executeActive(position)));
    }

    @GetMapping("/admin")
    @PreAuthorize("hasAuthority('BANNER_MANAGE')")
    public ResponseEntity<ApiResult<List<BannerData>>> listAll(
            @RequestParam(required = false) Boolean active,
            @RequestParam(required = false) String position
    ) {
        return ResponseEntity.ok(ApiResult.ok(getBannersUseCase.executeAll(active, position)));
    }

    @PostMapping
    @PreAuthorize("hasAuthority('BANNER_MANAGE')")
    public ResponseEntity<ApiResult<BannerData>> create(@Valid @RequestBody CreateBannerRequest req) {
        BannerData data = createBannerUseCase.execute(new CreateBannerCommand(
                req.title(), req.image(), req.url(), req.position(),
                req.active(), req.startDate(), req.endDate(), req.sortOrder()));
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResult.created(data));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAuthority('BANNER_MANAGE')")
    public ResponseEntity<ApiResult<BannerData>> update(
            @PathVariable Long id,
            @Valid @RequestBody UpdateBannerRequest req
    ) {
        BannerData data = updateBannerUseCase.execute(new UpdateBannerCommand(
                id, req.title(), req.image(), req.url(), req.position(),
                req.active(), req.startDate(), req.endDate(), req.sortOrder()));
        return ResponseEntity.ok(ApiResult.ok(data));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAuthority('BANNER_MANAGE')")
    public ResponseEntity<ApiResult<Void>> delete(@PathVariable Long id) {
        deleteBannerUseCase.execute(id);
        return ResponseEntity.ok(ApiResult.ok("Banner deleted successfully"));
    }
}
