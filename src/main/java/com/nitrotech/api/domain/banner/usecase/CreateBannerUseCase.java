package com.nitrotech.api.domain.banner.usecase;

import com.nitrotech.api.domain.banner.dto.BannerData;
import com.nitrotech.api.domain.banner.dto.CreateBannerCommand;
import com.nitrotech.api.domain.banner.repository.BannerRepository;
import com.nitrotech.api.shared.exception.DomainException;
import org.springframework.stereotype.Service;

@Service
public class CreateBannerUseCase {

    private final BannerRepository bannerRepository;

    public CreateBannerUseCase(BannerRepository bannerRepository) {
        this.bannerRepository = bannerRepository;
    }

    public BannerData execute(CreateBannerCommand command) {
        if (command.startDate() != null && command.endDate() != null
                && command.startDate().isAfter(command.endDate())) {
            throw new DomainException("INVALID_DATE_RANGE", "Start date must be before end date") {};
        }
        return bannerRepository.create(command);
    }
}
