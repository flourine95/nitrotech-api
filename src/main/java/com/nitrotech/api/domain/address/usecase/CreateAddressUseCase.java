package com.nitrotech.api.domain.address.usecase;

import com.nitrotech.api.domain.address.dto.AddressData;
import com.nitrotech.api.domain.address.dto.CreateAddressCommand;
import com.nitrotech.api.domain.address.exception.DefaultAddressConflictException;
import com.nitrotech.api.domain.address.repository.AddressRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataIntegrityViolationException;
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
            command.street(), false
        );

        AddressData address = addressRepository.create(userId, finalCommand);

        if (shouldBeDefault) {
            try {
                addressRepository.setAsDefault(userId, address.id());
            } catch (DataIntegrityViolationException ex) {
                if (ex.getMostSpecificCause().getMessage().contains("uq_addresses_one_default_per_user")) {
                    throw new DefaultAddressConflictException();
                }
                throw ex;
            }
        }

        return address;
    }
}
