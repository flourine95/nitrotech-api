package com.nitrotech.api.infrastructure.persistence.repository;

import com.nitrotech.api.infrastructure.persistence.entity.AddressEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface AddressJpaRepository extends JpaRepository<AddressEntity, Long> {

    List<AddressEntity> findByUserIdOrderByDefaultAddressDescCreatedAtDesc(Long userId);

    long countByUserId(Long userId);

    boolean existsByIdAndUserId(Long id, Long userId);

    @Modifying(flushAutomatically = true, clearAutomatically = true)
    @Query(value = "UPDATE addresses SET default_address = false, updated_at = CURRENT_TIMESTAMP WHERE user_id = :userId", nativeQuery = true)
    void unsetAllDefaultAddresses(@Param("userId") Long userId);

    @Modifying(flushAutomatically = true, clearAutomatically = true)
    @Query(value = "UPDATE addresses SET default_address = true, updated_at = CURRENT_TIMESTAMP WHERE user_id = :userId AND id = :addressId", nativeQuery = true)
    void setDefaultAddress(@Param("userId") Long userId, @Param("addressId") Long addressId);

    @Modifying(flushAutomatically = true, clearAutomatically = true)
    @Query(value = "DELETE FROM addresses WHERE id = :addressId AND default_address = false", nativeQuery = true)
    int deleteNonDefaultById(@Param("addressId") Long addressId);
}
