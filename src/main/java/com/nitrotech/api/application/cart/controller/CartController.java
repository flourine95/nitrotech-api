package com.nitrotech.api.application.cart.controller;

import com.nitrotech.api.application.cart.request.AddToCartRequest;
import com.nitrotech.api.application.cart.request.UpdateCartItemRequest;
import com.nitrotech.api.domain.cart.dto.CartData;
import com.nitrotech.api.domain.cart.dto.CartItemData;
import com.nitrotech.api.domain.cart.usecase.*;
import com.nitrotech.api.shared.response.ApiResult;
import com.nitrotech.api.shared.security.UserPrincipal;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/cart")
@RequiredArgsConstructor
public class CartController {

    private final GetCartUseCase getCartUseCase;
    private final AddToCartUseCase addToCartUseCase;
    private final UpdateCartItemUseCase updateCartItemUseCase;
    private final RemoveFromCartUseCase removeFromCartUseCase;
    private final ClearCartUseCase clearCartUseCase;

    @GetMapping
    public ResponseEntity<ApiResult<CartData>> get(@AuthenticationPrincipal UserPrincipal principal) {
        return ResponseEntity.ok(ApiResult.ok(getCartUseCase.execute(principal.id())));
    }

    @PostMapping("/items")
    public ResponseEntity<ApiResult<CartItemData>> addItem(
            @AuthenticationPrincipal UserPrincipal principal,
            @Valid @RequestBody AddToCartRequest req
    ) {
        return ResponseEntity.ok(ApiResult.ok(
                addToCartUseCase.execute(principal.id(), req.variantId(), req.quantity())));
    }

    @PutMapping("/items/{variantId}")
    public ResponseEntity<ApiResult<CartItemData>> updateItem(
            @AuthenticationPrincipal UserPrincipal principal,
            @PathVariable Long variantId,
            @Valid @RequestBody UpdateCartItemRequest req
    ) {
        return ResponseEntity.ok(ApiResult.ok(
                updateCartItemUseCase.execute(principal.id(), variantId, req.quantity())));
    }

    @DeleteMapping("/items/{variantId}")
    public ResponseEntity<ApiResult<Void>> removeItem(
            @AuthenticationPrincipal UserPrincipal principal,
            @PathVariable Long variantId
    ) {
        removeFromCartUseCase.execute(principal.id(), variantId);
        return ResponseEntity.ok(ApiResult.ok(null, "Item removed from cart"));
    }

    @DeleteMapping
    public ResponseEntity<ApiResult<Void>> clear(@AuthenticationPrincipal UserPrincipal principal) {
        clearCartUseCase.execute(principal.id());
        return ResponseEntity.ok(ApiResult.ok(null, "Cart cleared"));
    }
}