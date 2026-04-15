package com.nitrotech.api.domain.banner.usecase;

import com.nitrotech.api.domain.banner.dto.BannerData;
import com.nitrotech.api.domain.banner.dto.UpdateBannerCommand;
import com.nitrotech.api.domain.banner.repository.BannerRepository;
import com.nitrotech.api.shared.exception.DomainException;
import com.nitrotech.api.shared.exception.NotFoundException;
import org.springframework.stereotype.Service;

@Service
public class UpdateBannerUseCase {

    private final BannerRepository bannerRepository;

    public UpdateBannerUseCase(BannerRepository bannerRepository) {
        this.bannerRepository = bannerRepository;
    }

    public BannerData execute(UpdateBannerCommand command) {
        if (!bannerRepository.existsById(command.id())) {
            throw new NotFoundException("BANNER_NOT_FOUND", "Banner not found");
        }
        if (command.startDate() != null && command.endDate() != null
                && command.startDate().isAfter(command.endDate())) {
            throw new DomainException("INVALID_DATE_RANGE", "Start date must be before end date") {};
        }
        return bannerRepository.update(command);
    }
}
