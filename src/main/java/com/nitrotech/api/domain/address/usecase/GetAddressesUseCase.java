package com.nitrotech.api.domain.address.usecase;

import com.nitrotech.api.domain.address.dto.AddressData;
import com.nitrotech.api.domain.address.repository.AddressRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class GetAddressesUseCase {

    private final AddressRepository addressRepository;

    public List<AddressData> execute(Long userId) {
        return addressRepository.findByUserId(userId);
    }
}
