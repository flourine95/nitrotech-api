package com.nitrotech.api.application.cart.controller;

import com.nitrotech.api.application.cart.request.AddToCartRequest;
import com.nitrotech.api.application.cart.request.UpdateCartItemRequest;
import com.nitrotech.api.domain.cart.dto.CartData;
import com.nitrotech.api.domain.cart.dto.CartItemData;
import com.nitrotech.api.domain.cart.usecase.*;
import com.nitrotech.api.shared.response.ApiResult;
import com.nitrotech.api.shared.security.UserPrincipal;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/cart")
@Tag(name = "Cart", description = "Shopping cart management APIs")
@RequiredArgsConstructor
public class CartController {

    private final GetCartUseCase getCartUseCase;
    private final AddToCartUseCase addToCartUseCase;
    private final UpdateCartItemUseCase updateCartItemUseCase;
    private final RemoveFromCartUseCase removeFromCartUseCase;
    private final ClearCartUseCase clearCartUseCase;

    @Operation(summary = "Get cart", description = "Retrieve the current user's cart with all items and totals.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Cart retrieved successfully"),
            @ApiResponse(responseCode = "401", description = "Not authenticated", content = @Content(mediaType = "application/json"))
    })
    @GetMapping
    public ResponseEntity<ApiResult<CartData>> get(@AuthenticationPrincipal UserPrincipal principal) {
        return ResponseEntity.ok(ApiResult.ok(getCartUseCase.execute(principal.id())));
    }

    @Operation(summary = "Add item to cart", description = "Add a product variant to the cart. If the variant already exists, its quantity is incremented.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Item added to cart"),
            @ApiResponse(responseCode = "400", description = "Invalid input - validation error", content = @Content(mediaType = "application/json")),
            @ApiResponse(responseCode = "401", description = "Not authenticated", content = @Content(mediaType = "application/json")),
            @ApiResponse(responseCode = "404", description = "Variant not found", content = @Content(mediaType = "application/json")),
            @ApiResponse(responseCode = "409", description = "Insufficient stock", content = @Content(mediaType = "application/json"))
    })
    @PostMapping("/items")
    public ResponseEntity<ApiResult<CartItemData>> addItem(
            @AuthenticationPrincipal UserPrincipal principal,
            @Valid @RequestBody AddToCartRequest req
    ) {
        return ResponseEntity.ok(ApiResult.ok(
                addToCartUseCase.execute(principal.id(), req.variantId(), req.quantity())));
    }

    @Operation(summary = "Update cart item quantity", description = "Set the quantity of a specific variant in the cart. Set quantity to 0 to remove the item.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Cart item updated"),
            @ApiResponse(responseCode = "400", description = "Invalid input - validation error", content = @Content(mediaType = "application/json")),
            @ApiResponse(responseCode = "401", description = "Not authenticated", content = @Content(mediaType = "application/json")),
            @ApiResponse(responseCode = "404", description = "Item not found in cart", content = @Content(mediaType = "application/json")),
            @ApiResponse(responseCode = "409", description = "Insufficient stock", content = @Content(mediaType = "application/json"))
    })
    @PutMapping("/items/{variantId}")
    public ResponseEntity<ApiResult<CartItemData>> updateItem(
            @AuthenticationPrincipal UserPrincipal principal,
            @Parameter(description = "Variant ID") @PathVariable Long variantId,
            @Valid @RequestBody UpdateCartItemRequest req
    ) {
        return ResponseEntity.ok(ApiResult.ok(
                updateCartItemUseCase.execute(principal.id(), variantId, req.quantity())));
    }

    @Operation(summary = "Remove item from cart", description = "Remove a specific variant from the cart.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Item removed from cart"),
            @ApiResponse(responseCode = "401", description = "Not authenticated", content = @Content(mediaType = "application/json")),
            @ApiResponse(responseCode = "404", description = "Item not found in cart", content = @Content(mediaType = "application/json"))
    })
    @DeleteMapping("/items/{variantId}")
    public ResponseEntity<ApiResult<Void>> removeItem(
            @AuthenticationPrincipal UserPrincipal principal,
            @Parameter(description = "Variant ID") @PathVariable Long variantId
    ) {
        removeFromCartUseCase.execute(principal.id(), variantId);
        return ResponseEntity.ok(ApiResult.ok(null, "Item removed from cart"));
    }

    @Operation(summary = "Clear cart", description = "Remove all items from the current user's cart.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Cart cleared"),
            @ApiResponse(responseCode = "401", description = "Not authenticated", content = @Content(mediaType = "application/json"))
    })
    @DeleteMapping
    public ResponseEntity<ApiResult<Void>> clear(@AuthenticationPrincipal UserPrincipal principal) {
        clearCartUseCase.execute(principal.id());
        return ResponseEntity.ok(ApiResult.ok(null, "Cart cleared"));
    }
}
