package com.nitrotech.api.domain.user.usecase;

import com.nitrotech.api.domain.user.dto.AdminUserData;
import com.nitrotech.api.domain.user.dto.AdminUserFilter;
import com.nitrotech.api.domain.user.repository.AdminUserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class GetAdminUsersUseCase {

    private final AdminUserRepository adminUserRepository;

    public Page<AdminUserData> execute(AdminUserFilter filter, Pageable pageable) {
        return adminUserRepository.findAll(filter, pageable);
    }
}
