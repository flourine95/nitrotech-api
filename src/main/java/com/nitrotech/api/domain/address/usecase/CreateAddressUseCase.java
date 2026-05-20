package com.nitrotech.api.domain.address.usecase;

import com.nitrotech.api.domain.address.dto.AddressData;
import com.nitrotech.api.domain.address.dto.CreateAddressCommand;
import com.nitrotech.api.domain.address.repository.AddressRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class CreateAddressUseCase {

    private final AddressRepository addressRepository;

    @Transactional
    public AddressData execute(Long userId, CreateAddressCommand command) {
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

        if (shouldBeDefault) {
            addressRepository.setAsDefault(userId, address.id());
        }

        return address;
    }
}
