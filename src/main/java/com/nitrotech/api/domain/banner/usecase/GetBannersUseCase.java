package com.nitrotech.api.domain.banner.usecase;

import com.nitrotech.api.domain.banner.dto.BannerData;
import com.nitrotech.api.domain.banner.repository.BannerRepository;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class GetBannersUseCase {

    private final BannerRepository bannerRepository;

    public GetBannersUseCase(BannerRepository bannerRepository) {
        this.bannerRepository = bannerRepository;
    }

    // Public: chỉ lấy banner đang active và trong date range
    public List<BannerData> executeActive(String position) {
        return bannerRepository.findActive(position);
    }

    // Admin: lấy tất cả với filter
    public List<BannerData> executeAll(Boolean active, String position) {
        return bannerRepository.findAll(active, position);
    }
}
