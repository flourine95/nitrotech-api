package com.nitrotech.api.domain.address.repository;

import com.nitrotech.api.domain.address.dto.AddressData;
import com.nitrotech.api.domain.address.dto.CreateAddressCommand;
import com.nitrotech.api.domain.address.dto.UpdateAddressCommand;

import java.util.List;
import java.util.Optional;

public interface AddressRepository {

    List<AddressData> findByUserId(Long userId);

    Optional<AddressData> findById(Long id);

    Optional<AddressData> findByIdAndUserId(Long id, Long userId);

    AddressData create(Long userId, CreateAddressCommand command);

    AddressData update(Long id, UpdateAddressCommand command);

    boolean deleteNonDefault(Long id);

    void setAsDefault(Long userId, Long addressId);

    boolean belongsToUser(Long addressId, Long userId);

    long countByUserId(Long userId);
}
