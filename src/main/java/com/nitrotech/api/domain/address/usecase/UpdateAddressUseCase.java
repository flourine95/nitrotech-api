package com.nitrotech.api.domain.address.usecase;

import com.nitrotech.api.domain.address.dto.AddressData;
import com.nitrotech.api.domain.address.dto.UpdateAddressCommand;
import com.nitrotech.api.domain.address.exception.AddressAccessDeniedException;
import com.nitrotech.api.domain.address.exception.AddressNotFoundException;
import com.nitrotech.api.domain.address.repository.AddressRepository;

public class UpdateAddressUseCase {

    private final AddressRepository addressRepository;

    public UpdateAddressUseCase(AddressRepository addressRepository) {
        this.addressRepository = addressRepository;
    }

    public AddressData execute(Long userId, Long addressId, UpdateAddressCommand command) {
        // Check address exists
        AddressData existingAddress = addressRepository.findById(addressId)
            .orElseThrow(() -> AddressNotFoundException.withId(addressId));

        // Check ownership
        if (!existingAddress.userId().equals(userId)) {
            throw new AddressAccessDeniedException();
        }

        // Update address
        AddressData updatedAddress = addressRepository.update(addressId, command);

        // If set as default, unset other addresses
        if (command.defaultAddress() && !existingAddress.defaultAddress()) {
            addressRepository.setAsDefault(userId, addressId);
        }

        return updatedAddress;
    }
}
