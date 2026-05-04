package com.nitrotech.api.domain.address.usecase;

import com.nitrotech.api.domain.address.dto.AddressData;
import com.nitrotech.api.domain.address.dto.CreateAddressCommand;
import com.nitrotech.api.domain.address.repository.AddressRepository;

public class CreateAddressUseCase {

    private final AddressRepository addressRepository;

    public CreateAddressUseCase(AddressRepository addressRepository) {
        this.addressRepository = addressRepository;
    }

    public AddressData execute(Long userId, CreateAddressCommand command) {
        // If this is the first address, force it to be default
        long addressCount = addressRepository.countByUserId(userId);
        boolean shouldBeDefault = addressCount == 0 || command.defaultAddress();

        CreateAddressCommand finalCommand = new CreateAddressCommand(
            command.receiver(),
            command.phone(),
            command.province(),
            command.provinceCode(),
            command.district(),
            command.districtCode(),
            command.ward(),
            command.wardCode(),
            command.street(),
            shouldBeDefault
        );

        AddressData address = addressRepository.create(userId, finalCommand);

        // If set as default, unset other addresses
        if (shouldBeDefault) {
            addressRepository.setAsDefault(userId, address.id());
        }

        return address;
    }
}
