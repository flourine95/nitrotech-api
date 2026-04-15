package com.nitrotech.api.application.wishlist.controller;

import com.nitrotech.api.domain.auth.usecase.GetProfileUseCase;
import com.nitrotech.api.domain.wishlist.dto.WishlistItemData;
import com.nitrotech.api.domain.wishlist.usecase.GetWishlistUseCase;
import com.nitrotech.api.domain.wishlist.usecase.ToggleWishlistUseCase;
import com.nitrotech.api.shared.response.ApiResponse;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/wishlist")
public class WishlistController {

    private final GetWishlistUseCase getWishlistUseCase;
    private final ToggleWishlistUseCase toggleWishlistUseCase;
    private final GetProfileUseCase getProfileUseCase;

    public WishlistController(GetWishlistUseCase getWishlistUseCase,
                               ToggleWishlistUseCase toggleWishlistUseCase,
                               GetProfileUseCase getProfileUseCase) {
        this.getWishlistUseCase = getWishlistUseCase;
        this.toggleWishlistUseCase = toggleWishlistUseCase;
        this.getProfileUseCase = getProfileUseCase;
    }

    @GetMapping
    public ResponseEntity<ApiResponse<List<WishlistItemData>>> list(@AuthenticationPrincipal String email) {
        return ResponseEntity.ok(ApiResponse.ok(getWishlistUseCase.execute(userId(email))));
    }

    @PostMapping("/{productId}")
    public ResponseEntity<ApiResponse<Map<String, Boolean>>> toggle(
            @AuthenticationPrincipal String email,
            @PathVariable Long productId
    ) {
        boolean added = toggleWishlistUseCase.execute(userId(email), productId);
        String message = added ? "Added to wishlist" : "Removed from wishlist";
        return ResponseEntity.ok(ApiResponse.ok(Map.of("added", added), message));
    }

    private Long userId(String email) {
        return getProfileUseCase.executeByEmail(email).id();
    }
}
