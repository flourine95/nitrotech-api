package com.nitrotech.api.infrastructure.persistence.repository;

import com.nitrotech.api.domain.brand.dto.BrandData;
import com.nitrotech.api.domain.brand.dto.BrandFacets;
import com.nitrotech.api.domain.brand.dto.BrandFilter;
import com.nitrotech.api.domain.brand.dto.CreateBrandCommand;
import com.nitrotech.api.domain.brand.dto.UpdateBrandCommand;
import com.nitrotech.api.domain.brand.repository.BrandRepository;
import com.nitrotech.api.infrastructure.persistence.entity.BrandEntity;
import com.nitrotech.api.infrastructure.persistence.spec.BrandSpecification;
import com.nitrotech.api.shared.exception.NotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

@Repository
@RequiredArgsConstructor
public class BrandRepositoryImpl implements BrandRepository {

    private final BrandJpaRepository jpa;

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
        BrandEntity entity = jpa.findNotDeletedById(command.id())
                .orElseThrow(() -> new NotFoundException("BRAND_NOT_FOUND", "Brand not found"));
        if (command.name() != null) entity.setName(command.name());
        if (command.slug() != null) entity.setSlug(command.slug());
        if (command.logo() != null) entity.setLogo(command.logo());
        if (command.description() != null) entity.setDescription(command.description());
        if (command.active() != null) entity.setActive(command.active());
        return toData(jpa.save(entity));
    }

    @Override
    public Optional<BrandData> findNotDeletedById(Long id) {
        return jpa.findNotDeletedById(id).map(this::toData);
    }

    @Override
    public Optional<BrandData> findNotDeletedBySlug(String slug) {
        return jpa.findBySlugAndDeletedAtIsNull(slug).map(this::toData);
    }

    @Override
    public Optional<BrandData> findVisibleById(Long id) {
        return jpa.findVisibleById(id).map(this::toData);
    }

    @Override
    public Optional<BrandData> findVisibleBySlug(String slug) {
        return jpa.findBySlugAndActiveTrueAndDeletedAtIsNull(slug).map(this::toData);
    }

    @Override
    public Optional<BrandData> findDeletedById(Long id) {
        return jpa.findDeletedById(id).map(this::toData);
    }

    @Override
    public Page<BrandData> findAll(BrandFilter filter, Pageable pageable) {
        return jpa.findAll(BrandSpecification.from(filter), pageable).map(this::toData);
    }

    @Override
    public BrandFacets countFacets(String search) {
        List<Object[]> rows = jpa.countFacets(search);
        Object[] row = rows.isEmpty() ? new Object[]{0L, 0L, 0L} : rows.getFirst();
        return new BrandFacets(
                toLong(row[0]),
                toLong(row[1]),
                toLong(row[2])
        );
    }

    private long toLong(Object val) {
        if (val == null) return 0L;
        return ((Number) val).longValue();
    }

    @Override
    public boolean existsById(Long id) {
        return jpa.existsNotDeletedById(id);
    }

    @Override
    public boolean existsNotDeletedBySlug(String slug) {
        return jpa.existsNotDeletedBySlug(slug);
    }

    @Override
    public boolean existsNotDeletedBySlugAndIdNot(String slug, Long id) {
        return jpa.existsNotDeletedBySlugAndIdNot(slug, id);
    }

    @Override
    public void softDelete(Long id) {
        BrandEntity entity = jpa.findNotDeletedById(id)
                .orElseThrow(() -> new NotFoundException("BRAND_NOT_FOUND", "Brand not found"));
        entity.setDeletedAt(Instant.now());
        jpa.save(entity);
    }

    @Override
    public void restore(Long id) {
        BrandEntity entity = jpa.findDeletedById(id)
                .orElseThrow(() -> new NotFoundException("BRAND_NOT_FOUND", "Deleted brand not found"));
        entity.setDeletedAt(null);
        jpa.save(entity);
    }

    @Override
    public void hardDelete(Long id) {
        jpa.deleteById(id);
    }

    @Override
    @Transactional
    public List<Long> bulkSoftDelete(List<Long> ids) {
        List<Long> notDeletedIds = jpa.findAllNotDeletedByIds(ids).stream()
                .map(BrandEntity::getId).toList();
        if (!notDeletedIds.isEmpty()) {
            jpa.bulkSoftDelete(notDeletedIds, Instant.now());
        }
        return notDeletedIds;
    }

    @Override
    @Transactional
    public List<Long> bulkRestore(List<Long> ids) {
        List<Long> deletedIds = jpa.findDeletedIdsByIds(ids);
        if (!deletedIds.isEmpty()) {
            jpa.bulkRestore(deletedIds);
        }
        return deletedIds;
    }

    @Override
    @Transactional
    public List<Long> bulkHardDelete(List<Long> ids) {
        List<Long> deletedIds = jpa.findDeletedIdsByIds(ids);
        if (!deletedIds.isEmpty()) {
            jpa.deleteAllByIdInBatch(deletedIds);
        }
        return deletedIds;
    }

    private BrandData toData(BrandEntity e) {
        return new BrandData(e.getId(), e.getName(), e.getSlug(), e.getLogo(),
                e.getDescription(), e.isActive(), e.getCreatedAt(), e.getUpdatedAt());
    }
}
