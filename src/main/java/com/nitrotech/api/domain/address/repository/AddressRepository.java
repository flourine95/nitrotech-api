package com.nitrotech.api.domain.address.repository;

import com.nitrotech.api.domain.address.dto.AddressData;
import com.nitrotech.api.domain.address.dto.CreateAddressCommand;
import com.nitrotech.api.domain.address.dto.UpdateAddressCommand;

import java.util.List;
import java.util.Optional;

public interface AddressRepository {
    AddressData create(CreateAddressCommand command);
    AddressData update(UpdateAddressCommand command);
    Optional<AddressData> findByIdAndUserId(Long id, Long userId);
    List<AddressData> findByUserId(Long userId);
    void setDefault(Long id, Long userId);
    void delete(Long id, Long userId);
    boolean existsByIdAndUserId(Long id, Long userId);
}
