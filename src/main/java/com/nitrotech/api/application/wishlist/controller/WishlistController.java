package com.nitrotech.api.application.wishlist.controller;

import com.nitrotech.api.domain.wishlist.dto.WishlistItemData;
import com.nitrotech.api.domain.wishlist.usecase.GetWishlistUseCase;
import com.nitrotech.api.domain.wishlist.usecase.ToggleWishlistUseCase;
import com.nitrotech.api.shared.response.ApiResult;
import com.nitrotech.api.shared.security.UserPrincipal;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/wishlist")
@Tag(name = "Wishlist", description = "User wishlist management APIs")
@RequiredArgsConstructor
public class WishlistController {

    private final GetWishlistUseCase getWishlistUseCase;
    private final ToggleWishlistUseCase toggleWishlistUseCase;

    @Operation(summary = "Get wishlist", description = "Retrieve all products in the current user's wishlist.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Wishlist retrieved successfully"),
            @ApiResponse(responseCode = "401", description = "Not authenticated", content = @Content(mediaType = "application/json"))
    })
    @GetMapping
    public ResponseEntity<ApiResult<List<WishlistItemData>>> list(@AuthenticationPrincipal UserPrincipal principal) {
        return ResponseEntity.ok(ApiResult.ok(getWishlistUseCase.execute(principal.id())));
    }

    @Operation(summary = "Toggle wishlist item", description = "Add a product to the wishlist if it's not there, or remove it if it already is. Returns whether the product was added or removed.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Wishlist toggled successfully"),
            @ApiResponse(responseCode = "401", description = "Not authenticated", content = @Content(mediaType = "application/json")),
            @ApiResponse(responseCode = "404", description = "Product not found", content = @Content(mediaType = "application/json"))
    })
    @PostMapping("/{productId}")
    public ResponseEntity<ApiResult<Map<String, Boolean>>> toggle(
            @AuthenticationPrincipal UserPrincipal principal,
            @Parameter(description = "Product ID") @PathVariable Long productId
    ) {
        boolean added = toggleWishlistUseCase.execute(principal.id(), productId);
        String message = added ? "Added to wishlist" : "Removed from wishlist";
        return ResponseEntity.ok(ApiResult.ok(Map.of("added", added), message));
    }
}
