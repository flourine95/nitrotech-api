package com.nitrotech.api.domain.banner.usecase;

import com.nitrotech.api.domain.banner.repository.BannerRepository;
import com.nitrotech.api.shared.exception.NotFoundException;
import org.springframework.stereotype.Service;

@Service
public class DeleteBannerUseCase {

    private final BannerRepository bannerRepository;

    public DeleteBannerUseCase(BannerRepository bannerRepository) {
        this.bannerRepository = bannerRepository;
    }

    public void execute(Long id) {
        if (!bannerRepository.existsById(id)) {
            throw new NotFoundException("BANNER_NOT_FOUND", "Banner not found");
        }
        bannerRepository.delete(id);
    }
}
