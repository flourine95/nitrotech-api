package com.nitrotech.api.domain.address.usecase;

import com.nitrotech.api.domain.address.dto.AddressData;
import com.nitrotech.api.domain.address.repository.AddressRepository;

import java.util.List;

public class GetAddressesUseCase {

    private final AddressRepository addressRepository;

    public GetAddressesUseCase(AddressRepository addressRepository) {
        this.addressRepository = addressRepository;
    }

    public List<AddressData> execute(Long userId) {
        return addressRepository.findByUserId(userId);
    }
}
