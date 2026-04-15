package com.nitrotech.api.application.cart.controller;

import com.nitrotech.api.application.cart.request.AddToCartRequest;
import com.nitrotech.api.application.cart.request.UpdateCartItemRequest;
import com.nitrotech.api.domain.auth.usecase.GetProfileUseCase;
import com.nitrotech.api.domain.cart.dto.CartData;
import com.nitrotech.api.domain.cart.dto.CartItemData;
import com.nitrotech.api.domain.cart.usecase.*;
import com.nitrotech.api.shared.response.ApiResponse;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/cart")
public class CartController {

    private final GetCartUseCase getCartUseCase;
    private final AddToCartUseCase addToCartUseCase;
    private final UpdateCartItemUseCase updateCartItemUseCase;
    private final RemoveFromCartUseCase removeFromCartUseCase;
    private final ClearCartUseCase clearCartUseCase;
    private final GetProfileUseCase getProfileUseCase;

    public CartController(GetCartUseCase getCartUseCase, AddToCartUseCase addToCartUseCase,
                           UpdateCartItemUseCase updateCartItemUseCase,
                           RemoveFromCartUseCase removeFromCartUseCase,
                           ClearCartUseCase clearCartUseCase,
                           GetProfileUseCase getProfileUseCase) {
        this.getCartUseCase = getCartUseCase;
        this.addToCartUseCase = addToCartUseCase;
        this.updateCartItemUseCase = updateCartItemUseCase;
        this.removeFromCartUseCase = removeFromCartUseCase;
        this.clearCartUseCase = clearCartUseCase;
        this.getProfileUseCase = getProfileUseCase;
    }

    @GetMapping
    public ResponseEntity<ApiResponse<CartData>> get(@AuthenticationPrincipal String email) {
        return ResponseEntity.ok(ApiResponse.ok(getCartUseCase.execute(userId(email))));
    }

    @PostMapping("/items")
    public ResponseEntity<ApiResponse<CartItemData>> addItem(
            @AuthenticationPrincipal String email,
            @Valid @RequestBody AddToCartRequest req
    ) {
        return ResponseEntity.ok(ApiResponse.ok(
                addToCartUseCase.execute(userId(email), req.variantId(), req.quantity())));
    }

    @PutMapping("/items/{variantId}")
    public ResponseEntity<ApiResponse<CartItemData>> updateItem(
            @AuthenticationPrincipal String email,
            @PathVariable Long variantId,
            @Valid @RequestBody UpdateCartItemRequest req
    ) {
        return ResponseEntity.ok(ApiResponse.ok(
                updateCartItemUseCase.execute(userId(email), variantId, req.quantity())));
    }

    @DeleteMapping("/items/{variantId}")
    public ResponseEntity<ApiResponse<Void>> removeItem(
            @AuthenticationPrincipal String email,
            @PathVariable Long variantId
    ) {
        removeFromCartUseCase.execute(userId(email), variantId);
        return ResponseEntity.ok(ApiResponse.ok(null, "Item removed from cart"));
    }

    @DeleteMapping
    public ResponseEntity<ApiResponse<Void>> clear(@AuthenticationPrincipal String email) {
        clearCartUseCase.execute(userId(email));
        return ResponseEntity.ok(ApiResponse.ok(null, "Cart cleared"));
    }

    private Long userId(String email) {
        return getProfileUseCase.executeByEmail(email).id();
    }
}
