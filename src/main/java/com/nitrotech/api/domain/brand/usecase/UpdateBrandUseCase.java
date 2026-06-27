package com.nitrotech.api.domain.brand.usecase;

import com.nitrotech.api.domain.brand.dto.BrandData;
import com.nitrotech.api.domain.brand.dto.UpdateBrandCommand;
import com.nitrotech.api.domain.brand.exception.BrandNotFoundException;
import com.nitrotech.api.domain.brand.exception.BrandSlugExistsException;
import com.nitrotech.api.domain.brand.repository.BrandRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class UpdateBrandUseCase {

    private final BrandRepository brandRepository;

    @Transactional
    public BrandData execute(UpdateBrandCommand command) {
        if (!brandRepository.existsById(command.id())) {
            throw new BrandNotFoundException();
        }
        if (command.slug() != null && brandRepository.existsNotDeletedBySlugAndIdNot(command.slug(), command.id())) {
            throw new BrandSlugExistsException();
        }
        return brandRepository.update(command);
    }
}
