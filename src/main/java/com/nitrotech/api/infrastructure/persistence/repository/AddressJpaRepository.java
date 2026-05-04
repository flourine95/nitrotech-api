package com.nitrotech.api.infrastructure.persistence.repository;

import com.nitrotech.api.infrastructure.persistence.entity.AddressEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface AddressJpaRepository extends JpaRepository<AddressEntity, Long> {

    /**
     * Find all addresses by user ID, sorted by default first, then by createdAt DESC
     */
    List<AddressEntity> findByUserIdOrderByDefaultAddressDescCreatedAtDesc(Long userId);

    /**
     * Count addresses by user ID
     */
    long countByUserId(Long userId);

    /**
     * Check if address belongs to user
     */
    boolean existsByIdAndUserId(Long id, Long userId);

    /**
     * Set all addresses of user to non-default
     */
    @Modifying
    @Query("UPDATE AddressEntity a SET a.defaultAddress = false, a.updatedAt = CURRENT_TIMESTAMP WHERE a.userId = :userId")
    void unsetAllDefaultAddresses(@Param("userId") Long userId);

    /**
     * Set specific address as default
     */
    @Modifying
    @Query("UPDATE AddressEntity a SET a.defaultAddress = true, a.updatedAt = CURRENT_TIMESTAMP WHERE a.id = :addressId")
    void setAsDefault(@Param("addressId") Long addressId);
}
