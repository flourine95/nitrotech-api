package com.nitrotech.api.infrastructure.persistence.repository;

import com.nitrotech.api.domain.address.dto.AddressData;
import com.nitrotech.api.domain.address.dto.CreateAddressCommand;
import com.nitrotech.api.domain.address.dto.UpdateAddressCommand;
import com.nitrotech.api.domain.address.exception.AddressNotFoundException;
import com.nitrotech.api.domain.address.repository.AddressRepository;
import com.nitrotech.api.infrastructure.persistence.entity.AddressEntity;
import com.nitrotech.api.infrastructure.persistence.mapper.AddressMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

@Repository
@RequiredArgsConstructor
public class AddressRepositoryImpl implements AddressRepository {

    private final AddressJpaRepository jpa;
    private final AddressMapper mapper;

    @Override
    public List<AddressData> findByUserId(Long userId) {
        return jpa.findByUserIdOrderByDefaultAddressDescCreatedAtDesc(userId)
                .stream()
                .map(mapper::toData)
                .toList();
    }

    @Override
    public Optional<AddressData> findById(Long id) {
        return jpa.findById(id).map(mapper::toData);
    }

    @Override
    public Optional<AddressData> findByIdAndUserId(Long id, Long userId) {
        return jpa.findById(id)
                .filter(entity -> entity.getUserId().equals(userId))
                .map(mapper::toData);
    }

    @Override
    @Transactional
    public AddressData create(Long userId, CreateAddressCommand command) {
        AddressEntity entity = mapper.toEntity(command, userId);

        return mapper.toData(jpa.save(entity));
    }

    @Override
    @Transactional
    public AddressData update(Long id, UpdateAddressCommand command) {
        AddressEntity entity = jpa.findById(id)
                .orElseThrow(() -> AddressNotFoundException.withId(id));

        mapper.updateEntity(entity, command);

        return mapper.toData(jpa.save(entity));
    }

    @Override
    @Transactional
    public boolean deleteNonDefault(Long id) {
        return jpa.deleteNonDefaultById(id) == 1;
    }

    @Override
    @Transactional
    public void setAsDefault(Long userId, Long addressId) {
        jpa.unsetAllDefaultAddresses(userId, addressId);
    }

    @Override
    public boolean belongsToUser(Long addressId, Long userId) {
        return jpa.existsByIdAndUserId(addressId, userId);
    }

    @Override
    public long countByUserId(Long userId) {
        return jpa.countByUserId(userId);
    }
}
