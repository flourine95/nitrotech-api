package com.nitrotech.api.application.banner.controller;

import com.nitrotech.api.application.banner.request.CreateBannerRequest;
import com.nitrotech.api.application.banner.request.UpdateBannerRequest;
import com.nitrotech.api.domain.banner.dto.BannerData;
import com.nitrotech.api.domain.banner.dto.CreateBannerCommand;
import com.nitrotech.api.domain.banner.dto.UpdateBannerCommand;
import com.nitrotech.api.domain.banner.usecase.*;
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
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/banners")
@Tag(name = "Banners", description = "Banner management APIs")
@RequiredArgsConstructor
public class BannerController {

    private final GetBannersUseCase getBannersUseCase;
    private final CreateBannerUseCase createBannerUseCase;
    private final UpdateBannerUseCase updateBannerUseCase;
    private final DeleteBannerUseCase deleteBannerUseCase;

    @Operation(summary = "List active banners", description = "Public endpoint. Returns only active banners within their scheduled date range, optionally filtered by position.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Active banners retrieved successfully")
    })
    @GetMapping
    public ResponseEntity<ApiResult<List<BannerData>>> list(
            @Parameter(description = "Filter by banner position (e.g. homepage-hero, sidebar)") @RequestParam(required = false) String position
    ) {
        return ResponseEntity.ok(ApiResult.ok(getBannersUseCase.executeActive(position)));
    }

    @Operation(summary = "List all banners (admin)", description = "Admin endpoint. Returns all banners with optional filters by active status and position.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Banners retrieved successfully"),
            @ApiResponse(responseCode = "401", description = "Not authenticated", content = @Content(mediaType = "application/json")),
            @ApiResponse(responseCode = "403", description = "Insufficient permissions", content = @Content(mediaType = "application/json"))
    })
    @GetMapping("/admin")
    public ResponseEntity<ApiResult<List<BannerData>>> listAll(
            @Parameter(description = "Filter by active status") @RequestParam(required = false) Boolean active,
            @Parameter(description = "Filter by banner position") @RequestParam(required = false) String position
    ) {
        return ResponseEntity.ok(ApiResult.ok(getBannersUseCase.executeAll(active, position)));
    }

    @Operation(summary = "Create banner", description = "Create a new banner with scheduling and position settings.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "201", description = "Banner created successfully"),
            @ApiResponse(responseCode = "400", description = "Invalid input - validation error", content = @Content(mediaType = "application/json")),
            @ApiResponse(responseCode = "401", description = "Not authenticated", content = @Content(mediaType = "application/json"))
    })
    @PostMapping
    public ResponseEntity<ApiResult<BannerData>> create(@Valid @RequestBody CreateBannerRequest req) {
        BannerData data = createBannerUseCase.execute(new CreateBannerCommand(
                req.title(), req.image(), req.url(), req.position(),
                req.active(), req.startDate(), req.endDate(), req.sortOrder()));
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResult.created(data));
    }

    @Operation(summary = "Update banner", description = "Update an existing banner's content and scheduling.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Banner updated successfully"),
            @ApiResponse(responseCode = "400", description = "Invalid input - validation error", content = @Content(mediaType = "application/json")),
            @ApiResponse(responseCode = "404", description = "Banner not found", content = @Content(mediaType = "application/json"))
    })
    @PutMapping("/{id}")
    public ResponseEntity<ApiResult<BannerData>> update(
            @Parameter(description = "Banner ID") @PathVariable Long id,
            @Valid @RequestBody UpdateBannerRequest req
    ) {
        BannerData data = updateBannerUseCase.execute(new UpdateBannerCommand(
                id, req.title(), req.image(), req.url(), req.position(),
                req.active(), req.startDate(), req.endDate(), req.sortOrder()));
        return ResponseEntity.ok(ApiResult.ok(data));
    }

    @Operation(summary = "Delete banner", description = "Permanently remove a banner.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Banner deleted successfully"),
            @ApiResponse(responseCode = "404", description = "Banner not found", content = @Content(mediaType = "application/json"))
    })
    @DeleteMapping("/{id}")
    public ResponseEntity<ApiResult<Void>> delete(
            @Parameter(description = "Banner ID") @PathVariable Long id
    ) {
        deleteBannerUseCase.execute(id);
        return ResponseEntity.ok(ApiResult.ok(null, "Banner deleted successfully"));
    }
}
