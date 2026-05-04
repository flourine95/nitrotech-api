package com.nitrotech.api.domain.address.usecase;

import com.nitrotech.api.domain.address.dto.AddressData;
import com.nitrotech.api.domain.address.exception.AddressAccessDeniedException;
import com.nitrotech.api.domain.address.exception.AddressNotFoundException;
import com.nitrotech.api.domain.address.repository.AddressRepository;

public class SetDefaultAddressUseCase {

    private final AddressRepository addressRepository;

    public SetDefaultAddressUseCase(AddressRepository addressRepository) {
        this.addressRepository = addressRepository;
    }

    public void execute(Long userId, Long addressId) {
        // Check address exists
        AddressData address = addressRepository.findById(addressId)
            .orElseThrow(() -> AddressNotFoundException.withId(addressId));

        // Check ownership
        if (!address.userId().equals(userId)) {
            throw new AddressAccessDeniedException();
        }

        // Set as default (will unset others)
        addressRepository.setAsDefault(userId, addressId);
    }
}
