package com.nitrotech.api.domain.address.usecase;

import com.nitrotech.api.domain.address.dto.AddressData;
import com.nitrotech.api.domain.address.dto.CreateAddressCommand;
import com.nitrotech.api.domain.address.repository.AddressRepository;
import org.springframework.stereotype.Service;

@Service
public class CreateAddressUseCase {

    private final AddressRepository addressRepository;

    public CreateAddressUseCase(AddressRepository addressRepository) {
        this.addressRepository = addressRepository;
    }

    public AddressData execute(CreateAddressCommand command) {
        return addressRepository.create(command);
    }
}
