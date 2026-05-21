package com.nitrotech.api.application.address.controller;

import com.nitrotech.api.application.address.request.CreateAddressRequest;
import com.nitrotech.api.application.address.request.UpdateAddressRequest;
import com.nitrotech.api.domain.address.dto.AddressData;
import com.nitrotech.api.domain.address.dto.CreateAddressCommand;
import com.nitrotech.api.domain.address.dto.UpdateAddressCommand;
import com.nitrotech.api.domain.address.usecase.*;
import com.nitrotech.api.shared.response.ApiResult;
import com.nitrotech.api.shared.security.UserPrincipal;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/addresses")
@RequiredArgsConstructor
public class AddressController {

    private final GetAddressesUseCase getAddressesUseCase;
    private final CreateAddressUseCase createAddressUseCase;
    private final UpdateAddressUseCase updateAddressUseCase;
    private final DeleteAddressUseCase deleteAddressUseCase;
    private final SetDefaultAddressUseCase setDefaultAddressUseCase;

    @GetMapping
    public ResponseEntity<ApiResult<List<AddressData>>> getAddresses(@AuthenticationPrincipal UserPrincipal principal) {
        return ResponseEntity.ok(ApiResult.ok(getAddressesUseCase.execute(principal.id())));
    }

    @PostMapping
    public ResponseEntity<ApiResult<AddressData>> createAddress(
            @Valid @RequestBody CreateAddressRequest request,
            @AuthenticationPrincipal UserPrincipal principal
    ) {
        CreateAddressCommand command = new CreateAddressCommand(
                request.receiver(), request.phone(),
                request.province(), request.provinceCode(),
                request.district(), request.districtCode(),
                request.ward(), request.wardCode(),
                request.street(), request.defaultAddress()
        );
        AddressData address = createAddressUseCase.execute(principal.id(), command);
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResult.created(address));
    }

    @PutMapping("/{id}")
    public ResponseEntity<ApiResult<AddressData>> updateAddress(
            @PathVariable Long id,
            @Valid @RequestBody UpdateAddressRequest request,
            @AuthenticationPrincipal UserPrincipal principal
    ) {
        UpdateAddressCommand command = new UpdateAddressCommand(
                request.receiver(), request.phone(),
                request.province(), request.provinceCode(),
                request.district(), request.districtCode(),
                request.ward(), request.wardCode(),
                request.street(), request.defaultAddress()
        );
        AddressData address = updateAddressUseCase.execute(principal.id(), id, command);
        return ResponseEntity.ok(ApiResult.ok(address));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<ApiResult<Void>> deleteAddress(
            @PathVariable Long id,
            @AuthenticationPrincipal UserPrincipal principal
    ) {
        deleteAddressUseCase.execute(principal.id(), id);
        return ResponseEntity.ok(ApiResult.ok("Address deleted successfully"));
    }

    @PatchMapping("/{id}/set-default")
    public ResponseEntity<ApiResult<Void>> setDefaultAddress(
            @PathVariable Long id,
            @AuthenticationPrincipal UserPrincipal principal
    ) {
        setDefaultAddressUseCase.execute(principal.id(), id);
        return ResponseEntity.ok(ApiResult.ok("Default address updated"));
    }
}