package com.nitrotech.api.domain.banner.usecase;

import com.nitrotech.api.domain.shared.exception.InvalidDateRangeException;

import com.nitrotech.api.domain.banner.exception.BannerNotFoundException;

import com.nitrotech.api.domain.banner.dto.BannerData;
import com.nitrotech.api.domain.banner.dto.UpdateBannerCommand;
import com.nitrotech.api.domain.banner.repository.BannerRepository;
import com.nitrotech.api.shared.exception.DomainException;
import com.nitrotech.api.shared.exception.NotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class UpdateBannerUseCase {

    private final BannerRepository bannerRepository;

    public BannerData execute(UpdateBannerCommand command) {
        if (!bannerRepository.existsById(command.id())) {
            throw new BannerNotFoundException();
        }
        if (command.startDate() != null && command.endDate() != null
                && command.startDate().isAfter(command.endDate())) {
            throw new InvalidDateRangeException();
        }
        return bannerRepository.update(command);
    }
}
