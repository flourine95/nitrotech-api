package com.nitrotech.api.domain.user.usecase;

import com.nitrotech.api.domain.user.dto.AdminUserFacets;
import com.nitrotech.api.domain.user.dto.AdminUserFilter;
import com.nitrotech.api.domain.user.repository.AdminUserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class GetAdminUserFacetsUseCase {

    private final AdminUserRepository adminUserRepository;

    public AdminUserFacets execute(AdminUserFilter filter) {
        return adminUserRepository.countFacets(filter);
    }
}
