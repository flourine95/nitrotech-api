package com.nitrotech.api.domain.banner.repository;

import com.nitrotech.api.domain.banner.dto.BannerData;
import com.nitrotech.api.domain.banner.dto.CreateBannerCommand;
import com.nitrotech.api.domain.banner.dto.UpdateBannerCommand;

import java.util.List;
import java.util.Optional;

public interface BannerRepository {
    BannerData create(CreateBannerCommand command);
    BannerData update(UpdateBannerCommand command);
    Optional<BannerData> findById(Long id);
    List<BannerData> findActive(String position);
    List<BannerData> findAll(Boolean active, String position);
    boolean existsById(Long id);
    void delete(Long id);
}
