package com.nitrotech.api.domain.address.usecase;

import com.nitrotech.api.domain.address.dto.AddressData;
import com.nitrotech.api.domain.address.dto.UpdateAddressCommand;
import com.nitrotech.api.domain.address.repository.AddressRepository;
import com.nitrotech.api.shared.exception.NotFoundException;
import org.springframework.stereotype.Service;

@Service
public class UpdateAddressUseCase {

    private final AddressRepository addressRepository;

    public UpdateAddressUseCase(AddressRepository addressRepository) {
        this.addressRepository = addressRepository;
    }

    public AddressData execute(UpdateAddressCommand command) {
        if (!addressRepository.existsByIdAndUserId(command.id(), command.userId())) {
            throw new NotFoundException("ADDRESS_NOT_FOUND", "Address not found");
        }
        return addressRepository.update(command);
    }
}
