package com.nitrotech.api.domain.brand.usecase;

import com.nitrotech.api.domain.brand.dto.BrandData;
import com.nitrotech.api.domain.brand.dto.CreateBrandCommand;
import com.nitrotech.api.domain.brand.exception.BrandSlugExistsException;
import com.nitrotech.api.domain.brand.repository.BrandRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class CreateBrandUseCase {

    private final BrandRepository brandRepository;

    @Transactional
    public BrandData execute(CreateBrandCommand command) {
        if (brandRepository.existsNotDeletedBySlug(command.slug())) {
            throw new BrandSlugExistsException();
        }
        return brandRepository.create(command);
    }
}
