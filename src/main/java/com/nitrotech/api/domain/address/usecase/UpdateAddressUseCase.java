package com.nitrotech.api.domain.address.usecase;

import com.nitrotech.api.domain.address.dto.AddressData;
import com.nitrotech.api.domain.address.dto.UpdateAddressCommand;
import com.nitrotech.api.domain.address.exception.AddressAccessDeniedException;
import com.nitrotech.api.domain.address.exception.AddressNotFoundException;
import com.nitrotech.api.domain.address.exception.CannotUnsetDefaultAddressException;
import com.nitrotech.api.domain.address.repository.AddressRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class UpdateAddressUseCase {

    private final AddressRepository addressRepository;

    @Transactional
    public AddressData execute(Long userId, Long addressId, UpdateAddressCommand command) {
        AddressData existingAddress = addressRepository.findById(addressId)
                .orElseThrow(() -> AddressNotFoundException.withId(addressId));

        if (!existingAddress.userId().equals(userId)) {
            throw new AddressAccessDeniedException();
        }

        if (existingAddress.defaultAddress() && Boolean.FALSE.equals(command.defaultAddress())) {
            throw new CannotUnsetDefaultAddressException();
        }

        addressRepository.update(addressId, command);

        if (Boolean.TRUE.equals(command.defaultAddress()) && !existingAddress.defaultAddress()) {
            addressRepository.setAsDefault(userId, addressId);
        }

        return addressRepository.findById(addressId)
                .orElseThrow(() -> AddressNotFoundException.withId(addressId));
    }
}
