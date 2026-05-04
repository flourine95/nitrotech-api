package com.nitrotech.api.domain.address.usecase;

import com.nitrotech.api.domain.address.dto.AddressData;
import com.nitrotech.api.domain.address.exception.AddressAccessDeniedException;
import com.nitrotech.api.domain.address.exception.AddressNotFoundException;
import com.nitrotech.api.domain.address.exception.CannotDeleteDefaultAddressException;
import com.nitrotech.api.domain.address.repository.AddressRepository;

public class DeleteAddressUseCase {

    private final AddressRepository addressRepository;

    public DeleteAddressUseCase(AddressRepository addressRepository) {
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

        // Cannot delete default address
        if (address.defaultAddress()) {
            throw new CannotDeleteDefaultAddressException();
        }

        // Delete address
        addressRepository.delete(addressId);
    }
}
