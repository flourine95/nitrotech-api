package com.nitrotech.api.application.address.controller;

import com.nitrotech.api.application.address.request.CreateAddressRequest;
import com.nitrotech.api.application.address.request.UpdateAddressRequest;
import com.nitrotech.api.domain.address.dto.AddressData;
import com.nitrotech.api.domain.address.dto.CreateAddressCommand;
import com.nitrotech.api.domain.address.dto.UpdateAddressCommand;
import com.nitrotech.api.domain.address.usecase.*;
import com.nitrotech.api.shared.response.ApiResult;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/addresses")
@Tag(name = "Addresses", description = "User address management APIs")
@RequiredArgsConstructor
public class AddressController {

    private final GetAddressesUseCase getAddressesUseCase;
    private final CreateAddressUseCase createAddressUseCase;
    private final UpdateAddressUseCase updateAddressUseCase;
    private final DeleteAddressUseCase deleteAddressUseCase;
    private final SetDefaultAddressUseCase setDefaultAddressUseCase;

    @Operation(summary = "List addresses", description = "Get all addresses belonging to the current user.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Addresses retrieved successfully"),
            @ApiResponse(responseCode = "401", description = "Not authenticated", content = @Content(mediaType = "application/json"))
    })
    @GetMapping
    public ResponseEntity<ApiResult<List<AddressData>>> getAddresses(Authentication authentication) {
        Long userId = Long.parseLong(authentication.getName());
        return ResponseEntity.ok(ApiResult.ok(getAddressesUseCase.execute(userId)));
    }

    @Operation(summary = "Create address", description = "Add a new address for the current user.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "201", description = "Address created successfully"),
            @ApiResponse(responseCode = "400", description = "Invalid input - validation error", content = @Content(mediaType = "application/json")),
            @ApiResponse(responseCode = "401", description = "Not authenticated", content = @Content(mediaType = "application/json"))
    })
    @PostMapping
    public ResponseEntity<ApiResult<AddressData>> createAddress(
            @Valid @RequestBody CreateAddressRequest request,
            Authentication authentication
    ) {
        Long userId = Long.parseLong(authentication.getName());
        CreateAddressCommand command = new CreateAddressCommand(
                request.receiver(), request.phone(),
                request.province(), request.provinceCode(),
                request.district(), request.districtCode(),
                request.ward(), request.wardCode(),
                request.street(), request.defaultAddress()
        );
        AddressData address = createAddressUseCase.execute(userId, command);
        return ResponseEntity.status(201).body(ApiResult.ok(address, "Address created successfully"));
    }

    @Operation(summary = "Update address", description = "Update an existing address belonging to the current user.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Address updated successfully"),
            @ApiResponse(responseCode = "400", description = "Invalid input - validation error", content = @Content(mediaType = "application/json")),
            @ApiResponse(responseCode = "401", description = "Not authenticated", content = @Content(mediaType = "application/json")),
            @ApiResponse(responseCode = "404", description = "Address not found", content = @Content(mediaType = "application/json"))
    })
    @PutMapping("/{id}")
    public ResponseEntity<ApiResult<AddressData>> updateAddress(
            @Parameter(description = "Address ID") @PathVariable Long id,
            @Valid @RequestBody UpdateAddressRequest request,
            Authentication authentication
    ) {
        Long userId = Long.parseLong(authentication.getName());
        UpdateAddressCommand command = new UpdateAddressCommand(
                request.receiver(), request.phone(),
                request.province(), request.provinceCode(),
                request.district(), request.districtCode(),
                request.ward(), request.wardCode(),
                request.street(), request.defaultAddress()
        );
        AddressData address = updateAddressUseCase.execute(userId, id, command);
        return ResponseEntity.ok(ApiResult.ok(address, "Address updated successfully"));
    }

    @Operation(summary = "Delete address", description = "Remove an address belonging to the current user.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Address deleted successfully"),
            @ApiResponse(responseCode = "401", description = "Not authenticated", content = @Content(mediaType = "application/json")),
            @ApiResponse(responseCode = "404", description = "Address not found", content = @Content(mediaType = "application/json"))
    })
    @DeleteMapping("/{id}")
    public ResponseEntity<ApiResult<Void>> deleteAddress(
            @Parameter(description = "Address ID") @PathVariable Long id,
            Authentication authentication
    ) {
        Long userId = Long.parseLong(authentication.getName());
        deleteAddressUseCase.execute(userId, id);
        return ResponseEntity.ok(ApiResult.ok(null, "Address deleted successfully"));
    }

    @Operation(summary = "Set default address", description = "Mark an address as the default shipping address for the current user.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Default address updated"),
            @ApiResponse(responseCode = "401", description = "Not authenticated", content = @Content(mediaType = "application/json")),
            @ApiResponse(responseCode = "404", description = "Address not found", content = @Content(mediaType = "application/json"))
    })
    @PatchMapping("/{id}/set-default")
    public ResponseEntity<ApiResult<Void>> setDefaultAddress(
            @Parameter(description = "Address ID") @PathVariable Long id,
            Authentication authentication
    ) {
        Long userId = Long.parseLong(authentication.getName());
        setDefaultAddressUseCase.execute(userId, id);
        return ResponseEntity.ok(ApiResult.ok(null, "Default address updated"));
    }
}
