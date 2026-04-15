package com.nitrotech.api.application.address.controller;

import com.nitrotech.api.application.address.request.CreateAddressRequest;
import com.nitrotech.api.application.address.request.UpdateAddressRequest;
import com.nitrotech.api.domain.address.dto.AddressData;
import com.nitrotech.api.domain.address.dto.CreateAddressCommand;
import com.nitrotech.api.domain.address.dto.UpdateAddressCommand;
import com.nitrotech.api.domain.address.usecase.*;
import com.nitrotech.api.domain.auth.dto.UserProfileData;
import com.nitrotech.api.domain.auth.usecase.GetProfileUseCase;
import com.nitrotech.api.shared.response.ApiResponse;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/addresses")
public class AddressController {

    private final GetAddressesUseCase getAddressesUseCase;
    private final CreateAddressUseCase createAddressUseCase;
    private final UpdateAddressUseCase updateAddressUseCase;
    private final SetDefaultAddressUseCase setDefaultAddressUseCase;
    private final DeleteAddressUseCase deleteAddressUseCase;
    private final GetProfileUseCase getProfileUseCase;

    public AddressController(GetAddressesUseCase getAddressesUseCase,
                              CreateAddressUseCase createAddressUseCase,
                              UpdateAddressUseCase updateAddressUseCase,
                              SetDefaultAddressUseCase setDefaultAddressUseCase,
                              DeleteAddressUseCase deleteAddressUseCase,
                              GetProfileUseCase getProfileUseCase) {
        this.getAddressesUseCase = getAddressesUseCase;
        this.createAddressUseCase = createAddressUseCase;
        this.updateAddressUseCase = updateAddressUseCase;
        this.setDefaultAddressUseCase = setDefaultAddressUseCase;
        this.deleteAddressUseCase = deleteAddressUseCase;
        this.getProfileUseCase = getProfileUseCase;
    }

    @GetMapping
    public ResponseEntity<ApiResponse<List<AddressData>>> list(@AuthenticationPrincipal String email) {
        return ResponseEntity.ok(ApiResponse.ok(getAddressesUseCase.execute(userId(email))));
    }

    @PostMapping
    public ResponseEntity<ApiResponse<AddressData>> create(
            @AuthenticationPrincipal String email,
            @Valid @RequestBody CreateAddressRequest req
    ) {
        AddressData data = createAddressUseCase.execute(new CreateAddressCommand(
                userId(email), req.receiver(), req.phone(),
                req.province(), req.provinceCode(),
                req.district(), req.districtCode(),
                req.ward(), req.wardCode(),
                req.street(), req.defaultAddress()
        ));
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.created(data));
    }

    @PutMapping("/{id}")
    public ResponseEntity<ApiResponse<AddressData>> update(
            @AuthenticationPrincipal String email,
            @PathVariable Long id,
            @Valid @RequestBody UpdateAddressRequest req
    ) {
        AddressData data = updateAddressUseCase.execute(new UpdateAddressCommand(
                id, userId(email), req.receiver(), req.phone(),
                req.province(), req.provinceCode(),
                req.district(), req.districtCode(),
                req.ward(), req.wardCode(),
                req.street(), req.defaultAddress()
        ));
        return ResponseEntity.ok(ApiResponse.ok(data));
    }

    @PatchMapping("/{id}/default")
    public ResponseEntity<ApiResponse<Void>> setDefault(
            @AuthenticationPrincipal String email,
            @PathVariable Long id
    ) {
        setDefaultAddressUseCase.execute(id, userId(email));
        return ResponseEntity.ok(ApiResponse.ok(null, "Default address updated"));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<ApiResponse<Void>> delete(
            @AuthenticationPrincipal String email,
            @PathVariable Long id
    ) {
        deleteAddressUseCase.execute(id, userId(email));
        return ResponseEntity.ok(ApiResponse.ok(null, "Address deleted successfully"));
    }

    private Long userId(String email) {
        UserProfileData user = getProfileUseCase.executeByEmail(email);
        return user.id();
    }
}
