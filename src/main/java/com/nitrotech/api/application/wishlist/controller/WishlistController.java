package com.nitrotech.api.application.wishlist.controller;

import com.nitrotech.api.domain.wishlist.dto.WishlistItemData;
import com.nitrotech.api.domain.wishlist.usecase.GetWishlistUseCase;
import com.nitrotech.api.domain.wishlist.usecase.ToggleWishlistUseCase;
import com.nitrotech.api.shared.response.ApiResult;
import com.nitrotech.api.shared.security.UserPrincipal;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/wishlist")
@RequiredArgsConstructor
public class WishlistController {

    private final GetWishlistUseCase getWishlistUseCase;
    private final ToggleWishlistUseCase toggleWishlistUseCase;

    @GetMapping
    public ResponseEntity<ApiResult<List<WishlistItemData>>> list(@AuthenticationPrincipal UserPrincipal principal) {
        return ResponseEntity.ok(ApiResult.ok(getWishlistUseCase.execute(principal.id())));
    }

    @PostMapping("/{productId}")
    public ResponseEntity<ApiResult<Map<String, Boolean>>> toggle(
            @AuthenticationPrincipal UserPrincipal principal,
            @PathVariable Long productId
    ) {
        boolean added = toggleWishlistUseCase.execute(principal.id(), productId);
        String message = added ? "Added to wishlist" : "Removed from wishlist";
        return ResponseEntity.ok(ApiResult.ok(Map.of("added", added), message));
    }
}