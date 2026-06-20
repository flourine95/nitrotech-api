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
    @Modifying(flushAutomatically = true, clearAutomatically = true)
    @Query(value = "UPDATE addresses SET default_address = false, updated_at = CURRENT_TIMESTAMP WHERE user_id = :userId", nativeQuery = true)
    void unsetAllDefaultAddresses(@Param("userId") Long userId);

    /**
     * Set specific address as default
     */
    @Modifying(flushAutomatically = true, clearAutomatically = true)
    @Query(value = "UPDATE addresses SET default_address = true, updated_at = CURRENT_TIMESTAMP WHERE id = :addressId", nativeQuery = true)
    void setAsDefault(@Param("addressId") Long addressId);
}
