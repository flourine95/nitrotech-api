package com.nitrotech.api.infrastructure.persistence.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.Instant;

@Entity
@Table(
        name = "carrier_address_mappings",
        uniqueConstraints = @UniqueConstraint(
                name = "uq_carrier_address_mapping",
                columnNames = {"provider", "province_code", "district_code", "ward_code"}
        )
)
@Getter @Setter @NoArgsConstructor
public class CarrierAddressMappingEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 30)
    private String provider;

    @Column(name = "province_code", nullable = false, length = 50)
    private String provinceCode;

    @Column(name = "district_code", nullable = false, length = 50)
    private String districtCode;

    @Column(name = "ward_code", nullable = false, length = 50)
    private String wardCode;

    @Column(name = "province_name", nullable = false)
    private String provinceName;

    @Column(name = "district_name", nullable = false)
    private String districtName;

    @Column(name = "ward_name", nullable = false)
    private String wardName;

    @Column(name = "carrier_province_id", length = 50)
    private String carrierProvinceId;

    @Column(name = "carrier_district_id", length = 50)
    private String carrierDistrictId;

    @Column(name = "carrier_ward_code", length = 50)
    private String carrierWardCode;

    @Column(name = "carrier_province_name")
    private String carrierProvinceName;

    @Column(name = "carrier_district_name")
    private String carrierDistrictName;

    @Column(name = "carrier_ward_name")
    private String carrierWardName;

    @Column(nullable = false, length = 20)
    private String confidence = "verified";

    @Column(nullable = false)
    private boolean active = true;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;
}
