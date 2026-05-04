package com.nitrotech.api.domain.address.repository;

import com.nitrotech.api.domain.address.dto.AddressData;
import com.nitrotech.api.domain.address.dto.CreateAddressCommand;
import com.nitrotech.api.domain.address.dto.UpdateAddressCommand;

import java.util.List;
import java.util.Optional;

public interface AddressRepository {

    /**
     * Get all addresses for a user, sorted by default first, then by createdAt DESC
     */
    List<AddressData> findByUserId(Long userId);

    /**
     * Find address by ID
     */
    Optional<AddressData> findById(Long id);

    /**
     * Find address by ID and user ID
     */
    Optional<AddressData> findByIdAndUserId(Long id, Long userId);

    /**
     * Create new address
     */
    AddressData create(Long userId, CreateAddressCommand command);

    /**
     * Update address
     */
    AddressData update(Long id, UpdateAddressCommand command);

    /**
     * Delete address
     */
    void delete(Long id);

    /**
     * Set address as default (and unset others)
     */
    void setAsDefault(Long userId, Long addressId);

    /**
     * Check if address belongs to user
     */
    boolean belongsToUser(Long addressId, Long userId);

    /**
     * Count addresses for user
     */
    long countByUserId(Long userId);
}
