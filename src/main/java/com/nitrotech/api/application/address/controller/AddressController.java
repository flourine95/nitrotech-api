package com.nitrotech.api.application.address.controller;

import com.nitrotech.api.application.address.request.CreateAddressRequest;
import com.nitrotech.api.application.address.request.UpdateAddressRequest;
import com.nitrotech.api.domain.address.dto.AddressData;
import com.nitrotech.api.domain.address.dto.CreateAddressCommand;
import com.nitrotech.api.domain.address.dto.UpdateAddressCommand;
import com.nitrotech.api.domain.address.usecase.*;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/addresses")
public class AddressController {

    private final GetAddressesUseCase getAddressesUseCase;
    private final CreateAddressUseCase createAddressUseCase;
    private final UpdateAddressUseCase updateAddressUseCase;
    private final DeleteAddressUseCase deleteAddressUseCase;
    private final SetDefaultAddressUseCase setDefaultAddressUseCase;

    public AddressController(
        GetAddressesUseCase getAddressesUseCase,
        CreateAddressUseCase createAddressUseCase,
        UpdateAddressUseCase updateAddressUseCase,
        DeleteAddressUseCase deleteAddressUseCase,
        SetDefaultAddressUseCase setDefaultAddressUseCase
    ) {
        this.getAddressesUseCase = getAddressesUseCase;
        this.createAddressUseCase = createAddressUseCase;
        this.updateAddressUseCase = updateAddressUseCase;
        this.deleteAddressUseCase = deleteAddressUseCase;
        this.setDefaultAddressUseCase = setDefaultAddressUseCase;
    }

    @GetMapping
    public ResponseEntity<Map<String, Object>> getAddresses(Authentication authentication) {
        Long userId = Long.parseLong(authentication.getName());
        List<AddressData> addresses = getAddressesUseCase.execute(userId);
        return ResponseEntity.ok(Map.of("data", addresses));
    }

    @PostMapping
    public ResponseEntity<Map<String, Object>> createAddress(
        @Valid @RequestBody CreateAddressRequest request,
        Authentication authentication
    ) {
        Long userId = Long.parseLong(authentication.getName());
        
        CreateAddressCommand command = new CreateAddressCommand(
            request.receiver(),
            request.phone(),
            request.province(),
            request.provinceCode(),
            request.district(),
            request.districtCode(),
            request.ward(),
            request.wardCode(),
            request.street(),
            request.defaultAddress()
        );
        
        AddressData address = createAddressUseCase.execute(userId, command);
        
        return ResponseEntity.status(201).body(Map.of(
            "data", address,
            "message", "Address created successfully"
        ));
    }

    @PutMapping("/{id}")
    public ResponseEntity<Map<String, Object>> updateAddress(
        @PathVariable Long id,
        @Valid @RequestBody UpdateAddressRequest request,
        Authentication authentication
    ) {
        Long userId = Long.parseLong(authentication.getName());
        
        UpdateAddressCommand command = new UpdateAddressCommand(
            request.receiver(),
            request.phone(),
            request.province(),
            request.provinceCode(),
            request.district(),
            request.districtCode(),
            request.ward(),
            request.wardCode(),
            request.street(),
            request.defaultAddress()
        );
        
        AddressData address = updateAddressUseCase.execute(userId, id, command);
        
        return ResponseEntity.ok(Map.of(
            "data", address,
            "message", "Address updated successfully"
        ));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Map<String, Object>> deleteAddress(
        @PathVariable Long id,
        Authentication authentication
    ) {
        Long userId = Long.parseLong(authentication.getName());
        deleteAddressUseCase.execute(userId, id);
        return ResponseEntity.ok(Map.of("message", "Address deleted successfully"));
    }

    @PatchMapping("/{id}/set-default")
    public ResponseEntity<Map<String, Object>> setDefaultAddress(
        @PathVariable Long id,
        Authentication authentication
    ) {
        Long userId = Long.parseLong(authentication.getName());
        setDefaultAddressUseCase.execute(userId, id);
        return ResponseEntity.ok(Map.of("message", "Default address updated"));
    }
}
