package com.nitrotech.api.infrastructure.persistence.repository;

import com.nitrotech.api.domain.brand.dto.BrandData;
import com.nitrotech.api.domain.brand.dto.BrandFilter;
import com.nitrotech.api.domain.brand.dto.CreateBrandCommand;
import com.nitrotech.api.domain.brand.dto.UpdateBrandCommand;
import com.nitrotech.api.domain.brand.repository.BrandRepository;
import com.nitrotech.api.infrastructure.persistence.entity.BrandEntity;
import com.nitrotech.api.infrastructure.persistence.spec.BrandSpecification;
import com.nitrotech.api.shared.exception.NotFoundException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.Optional;

@Repository
public class BrandRepositoryImpl implements BrandRepository {

    private final BrandJpaRepository jpa;

    public BrandRepositoryImpl(BrandJpaRepository jpa) {
        this.jpa = jpa;
    }

    @Override
    public BrandData create(CreateBrandCommand command) {
        BrandEntity entity = new BrandEntity();
        entity.setName(command.name());
        entity.setSlug(command.slug());
        entity.setLogo(command.logo());
        entity.setDescription(command.description());
        entity.setActive(command.active());
        return toData(jpa.save(entity));
    }

    @Override
    public BrandData update(UpdateBrandCommand command) {
        BrandEntity entity = jpa.findActiveById(command.id())
                .orElseThrow(() -> new NotFoundException("BRAND_NOT_FOUND", "Brand not found"));
        if (command.name() != null) entity.setName(command.name());
        if (command.slug() != null) entity.setSlug(command.slug());
        if (command.logo() != null) entity.setLogo(command.logo());
        if (command.description() != null) entity.setDescription(command.description());
        if (command.active() != null) entity.setActive(command.active());
        entity.setUpdatedAt(LocalDateTime.now());
        return toData(jpa.save(entity));
    }

    @Override
    public Optional<BrandData> findById(Long id) {
        return jpa.findActiveById(id).map(this::toData);
    }

    @Override
    public Page<BrandData> findAll(BrandFilter filter, Pageable pageable) {
        return jpa.findAll(BrandSpecification.from(filter), pageable).map(this::toData);
    }

    @Override
    public boolean existsById(Long id) {
        return jpa.existsActiveById(id);
    }

    @Override
    public boolean existsBySlug(String slug) {
        return jpa.existsBySlug(slug);
    }

    @Override
    public boolean existsBySlugAndIdNot(String slug, Long id) {
        return jpa.existsBySlugAndIdNot(slug, id);
    }

    @Override
    public void softDelete(Long id) {
        jpa.findActiveById(id).ifPresent(e -> {
            e.setDeletedAt(LocalDateTime.now());
            jpa.save(e);
        });
    }

    private BrandData toData(BrandEntity e) {
        return new BrandData(e.getId(), e.getName(), e.getSlug(), e.getLogo(),
                e.getDescription(), e.isActive(), e.getCreatedAt(), e.getUpdatedAt());
    }
}
