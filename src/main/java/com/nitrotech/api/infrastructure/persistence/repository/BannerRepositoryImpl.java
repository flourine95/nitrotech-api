package com.nitrotech.api.infrastructure.persistence.repository;

import com.nitrotech.api.domain.banner.dto.BannerData;
import com.nitrotech.api.domain.banner.dto.CreateBannerCommand;
import com.nitrotech.api.domain.banner.dto.UpdateBannerCommand;
import com.nitrotech.api.domain.banner.repository.BannerRepository;
import com.nitrotech.api.infrastructure.persistence.entity.BannerEntity;
import com.nitrotech.api.shared.exception.NotFoundException;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public class BannerRepositoryImpl implements BannerRepository {

    private final BannerJpaRepository jpa;

    public BannerRepositoryImpl(BannerJpaRepository jpa) {
        this.jpa = jpa;
    }

    @Override
    public BannerData create(CreateBannerCommand command) {
        BannerEntity entity = new BannerEntity();
        entity.setTitle(command.title());
        entity.setImage(command.image());
        entity.setUrl(command.url());
        entity.setPosition(command.position());
        entity.setActive(command.active());
        entity.setStartDate(command.startDate());
        entity.setEndDate(command.endDate());
        entity.setSortOrder(command.sortOrder());
        return toData(jpa.save(entity));
    }

    @Override
    public BannerData update(UpdateBannerCommand command) {
        BannerEntity entity = jpa.findById(command.id())
                .orElseThrow(() -> new NotFoundException("BANNER_NOT_FOUND", "Banner not found"));
        if (command.title() != null) entity.setTitle(command.title());
        if (command.image() != null) entity.setImage(command.image());
        if (command.url() != null) entity.setUrl(command.url());
        if (command.position() != null) entity.setPosition(command.position());
        if (command.active() != null) entity.setActive(command.active());
        if (command.startDate() != null) entity.setStartDate(command.startDate());
        if (command.endDate() != null) entity.setEndDate(command.endDate());
        if (command.sortOrder() != null) entity.setSortOrder(command.sortOrder());
        entity.setUpdatedAt(LocalDateTime.now());
        return toData(jpa.save(entity));
    }

    @Override
    public Optional<BannerData> findById(Long id) {
        return jpa.findById(id).map(this::toData);
    }

    @Override
    public List<BannerData> findActive(String position) {
        return jpa.findActive(position, LocalDateTime.now()).stream().map(this::toData).toList();
    }

    @Override
    public List<BannerData> findAll(Boolean active, String position) {
        return jpa.findAllFiltered(active, position).stream().map(this::toData).toList();
    }

    @Override
    public boolean existsById(Long id) {
        return jpa.existsById(id);
    }

    @Override
    public void delete(Long id) {
        jpa.deleteById(id);
    }

    private BannerData toData(BannerEntity e) {
        return new BannerData(e.getId(), e.getTitle(), e.getImage(), e.getUrl(),
                e.getPosition(), e.isActive(), e.getStartDate(), e.getEndDate(),
                e.getSortOrder(), e.getCreatedAt(), e.getUpdatedAt());
    }
}
