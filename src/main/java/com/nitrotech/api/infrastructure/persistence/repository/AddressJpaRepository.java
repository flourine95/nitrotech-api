package com.nitrotech.api.infrastructure.persistence.repository;

import com.nitrotech.api.infrastructure.persistence.entity.AddressEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface AddressJpaRepository extends JpaRepository<AddressEntity, Long> {

    List<AddressEntity> findByUserIdOrderByDefaultAddressDescCreatedAtDesc(Long userId);

    Optional<AddressEntity> findByIdAndUserId(Long id, Long userId);

    boolean existsByIdAndUserId(Long id, Long userId);

    @Modifying
    @Query("UPDATE AddressEntity a SET a.defaultAddress = FALSE WHERE a.userId = :userId")
    void clearDefaultByUserId(@Param("userId") Long userId);
}
