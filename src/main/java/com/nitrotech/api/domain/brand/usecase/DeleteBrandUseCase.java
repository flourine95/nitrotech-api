package com.nitrotech.api.domain.brand.usecase;

import com.nitrotech.api.domain.brand.exception.BrandNotFoundException;

import com.nitrotech.api.domain.brand.repository.BrandRepository;
import com.nitrotech.api.shared.exception.NotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class DeleteBrandUseCase {

    private final BrandRepository brandRepository;

    public void execute(Long id) {
        if (!brandRepository.existsById(id)) {
            throw new BrandNotFoundException();
        }
        brandRepository.softDelete(id);
    }
}
