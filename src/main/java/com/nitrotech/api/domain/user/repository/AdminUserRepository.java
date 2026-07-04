package com.nitrotech.api.domain.user.repository;

import com.nitrotech.api.domain.user.dto.AdminUserData;
import com.nitrotech.api.domain.user.dto.AdminUserFacets;
import com.nitrotech.api.domain.user.dto.AdminUserFilter;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

public interface AdminUserRepository {
    Page<AdminUserData> findAll(AdminUserFilter filter, Pageable pageable);

    Optional<AdminUserData> findById(Long id);

    AdminUserFacets countFacets(AdminUserFilter filter);

    AdminUserData create(String name, String email, String phone, String status, String hashedPassword, Set<String> roleSlugs);

    AdminUserData update(Long id, String name, String email, String phone, String status);

    List<Long> bulkSoftDelete(List<Long> ids);

    List<Long> bulkUpdateStatus(List<Long> ids, String status);

    List<Long> bulkRestore(List<Long> ids);

    Map<Long, String> findStatusesByIds(List<Long> ids);

    List<String> findEmailsByIds(List<Long> ids);
}
