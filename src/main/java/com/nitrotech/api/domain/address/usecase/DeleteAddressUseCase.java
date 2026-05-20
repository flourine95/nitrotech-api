package com.nitrotech.api.domain.address.usecase;

import com.nitrotech.api.domain.address.dto.AddressData;
import com.nitrotech.api.domain.address.exception.AddressAccessDeniedException;
import com.nitrotech.api.domain.address.exception.AddressNotFoundException;
import com.nitrotech.api.domain.address.exception.CannotDeleteDefaultAddressException;
import com.nitrotech.api.domain.address.repository.AddressRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class DeleteAddressUseCase {

    private final AddressRepository addressRepository;

    public void execute(Long userId, Long addressId) {
        AddressData address = addressRepository.findById(addressId)
            .orElseThrow(() -> AddressNotFoundException.withId(addressId));

        if (!address.userId().equals(userId)) {
            throw new AddressAccessDeniedException();
        }

        if (address.defaultAddress()) {
            throw new CannotDeleteDefaultAddressException();
        }

        addressRepository.delete(addressId);
    }
}
