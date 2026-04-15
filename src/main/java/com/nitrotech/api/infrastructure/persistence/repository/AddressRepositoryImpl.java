package com.nitrotech.api.infrastructure.persistence.repository;

import com.nitrotech.api.domain.address.dto.AddressData;
import com.nitrotech.api.domain.address.dto.CreateAddressCommand;
import com.nitrotech.api.domain.address.dto.UpdateAddressCommand;
import com.nitrotech.api.domain.address.repository.AddressRepository;
import com.nitrotech.api.infrastructure.persistence.entity.AddressEntity;
import com.nitrotech.api.shared.exception.NotFoundException;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public class AddressRepositoryImpl implements AddressRepository {

    private final AddressJpaRepository jpa;

    public AddressRepositoryImpl(AddressJpaRepository jpa) {
        this.jpa = jpa;
    }

    @Override
    @Transactional
    public AddressData create(CreateAddressCommand command) {
        if (command.defaultAddress()) {
            jpa.clearDefaultByUserId(command.userId());
        }
        AddressEntity entity = new AddressEntity();
        entity.setUserId(command.userId());
        entity.setReceiver(command.receiver());
        entity.setPhone(command.phone());
        entity.setProvince(command.province());
        entity.setProvinceCode(command.provinceCode());
        entity.setDistrict(command.district());
        entity.setDistrictCode(command.districtCode());
        entity.setWard(command.ward());
        entity.setWardCode(command.wardCode());
        entity.setStreet(command.street());
        entity.setDefaultAddress(command.defaultAddress());
        return toData(jpa.save(entity));
    }

    @Override
    @Transactional
    public AddressData update(UpdateAddressCommand command) {
        AddressEntity entity = jpa.findByIdAndUserId(command.id(), command.userId())
                .orElseThrow(() -> new NotFoundException("ADDRESS_NOT_FOUND", "Address not found"));
        if (command.receiver() != null) entity.setReceiver(command.receiver());
        if (command.phone() != null) entity.setPhone(command.phone());
        if (command.province() != null) entity.setProvince(command.province());
        if (command.provinceCode() != null) entity.setProvinceCode(command.provinceCode());
        if (command.district() != null) entity.setDistrict(command.district());
        if (command.districtCode() != null) entity.setDistrictCode(command.districtCode());
        if (command.ward() != null) entity.setWard(command.ward());
        if (command.wardCode() != null) entity.setWardCode(command.wardCode());
        if (command.street() != null) entity.setStreet(command.street());
        if (command.defaultAddress() != null && command.defaultAddress()) {
            jpa.clearDefaultByUserId(command.userId());
            entity.setDefaultAddress(true);
        }
        entity.setUpdatedAt(LocalDateTime.now());
        return toData(jpa.save(entity));
    }

    @Override
    public Optional<AddressData> findByIdAndUserId(Long id, Long userId) {
        return jpa.findByIdAndUserId(id, userId).map(this::toData);
    }

    @Override
    public List<AddressData> findByUserId(Long userId) {
        return jpa.findByUserIdOrderByDefaultAddressDescCreatedAtDesc(userId)
                .stream().map(this::toData).toList();
    }

    @Override
    @Transactional
    public void setDefault(Long id, Long userId) {
        jpa.clearDefaultByUserId(userId);
        jpa.findByIdAndUserId(id, userId).ifPresent(e -> {
            e.setDefaultAddress(true);
            e.setUpdatedAt(LocalDateTime.now());
            jpa.save(e);
        });
    }

    @Override
    public void delete(Long id, Long userId) {
        jpa.findByIdAndUserId(id, userId).ifPresent(jpa::delete);
    }

    @Override
    public boolean existsByIdAndUserId(Long id, Long userId) {
        return jpa.existsByIdAndUserId(id, userId);
    }

    private AddressData toData(AddressEntity e) {
        return new AddressData(
                e.getId(), e.getUserId(), e.getReceiver(), e.getPhone(),
                e.getProvince(), e.getProvinceCode(), e.getDistrict(), e.getDistrictCode(),
                e.getWard(), e.getWardCode(), e.getStreet(), e.isDefaultAddress(),
                e.getCreatedAt(), e.getUpdatedAt()
        );
    }
}
