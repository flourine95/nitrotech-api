package com.nitrotech.api.infrastructure.persistence.repository;

import com.nitrotech.api.infrastructure.persistence.entity.CarrierAddressMappingEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface CarrierAddressMappingJpaRepository extends JpaRepository<CarrierAddressMappingEntity, Long> {
    Optional<CarrierAddressMappingEntity> findByProviderIgnoreCaseAndProvinceCodeAndDistrictCodeAndWardCodeAndActiveTrue(
            String provider,
            String provinceCode,
            String districtCode,
            String wardCode
    );
}
