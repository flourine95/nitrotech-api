package com.nitrotech.api.domain.address.usecase;

import com.nitrotech.api.domain.address.repository.AddressRepository;
import com.nitrotech.api.shared.exception.NotFoundException;
import org.springframework.stereotype.Service;

@Service
public class DeleteAddressUseCase {

    private final AddressRepository addressRepository;

    public DeleteAddressUseCase(AddressRepository addressRepository) {
        this.addressRepository = addressRepository;
    }

    public void execute(Long id, Long userId) {
        if (!addressRepository.existsByIdAndUserId(id, userId)) {
            throw new NotFoundException("ADDRESS_NOT_FOUND", "Address not found");
        }
        addressRepository.delete(id, userId);
    }
}
