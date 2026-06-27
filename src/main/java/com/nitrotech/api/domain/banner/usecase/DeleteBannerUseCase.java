package com.nitrotech.api.domain.banner.usecase;

import com.nitrotech.api.domain.banner.exception.BannerNotFoundException;
import com.nitrotech.api.domain.banner.repository.BannerRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class DeleteBannerUseCase {

    private final BannerRepository bannerRepository;

    public void execute(Long id) {
        if (!bannerRepository.existsById(id)) {
            throw new BannerNotFoundException();
        }
        bannerRepository.delete(id);
    }
}
