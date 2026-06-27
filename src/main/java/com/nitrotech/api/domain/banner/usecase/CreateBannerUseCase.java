package com.nitrotech.api.domain.banner.usecase;

import com.nitrotech.api.domain.shared.exception.InvalidDateRangeException;

import com.nitrotech.api.domain.banner.dto.BannerData;
import com.nitrotech.api.domain.banner.dto.CreateBannerCommand;
import com.nitrotech.api.domain.banner.repository.BannerRepository;
import com.nitrotech.api.shared.exception.DomainException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class CreateBannerUseCase {

    private final BannerRepository bannerRepository;

    public BannerData execute(CreateBannerCommand command) {
        if (command.startDate() != null && command.endDate() != null
                && command.startDate().isAfter(command.endDate())) {
            throw new InvalidDateRangeException();
        }
        return bannerRepository.create(command);
    }
}
