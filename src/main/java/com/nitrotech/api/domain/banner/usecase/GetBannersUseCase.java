package com.nitrotech.api.domain.banner.usecase;

import com.nitrotech.api.domain.banner.dto.BannerData;
import com.nitrotech.api.domain.banner.repository.BannerRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class GetBannersUseCase {

    private final BannerRepository bannerRepository;

    public List<BannerData> executeActive(String position) {
        return bannerRepository.findActive(position);
    }

    public List<BannerData> executeAll(Boolean active, String position) {
        return bannerRepository.findAll(active, position);
    }
}
