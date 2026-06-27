package com.nitrotech.api.infrastructure.persistence.mapper;

import com.nitrotech.api.domain.shipping.dto.ShipmentData;
import com.nitrotech.api.domain.shipping.ShipmentStatus;
import com.nitrotech.api.infrastructure.persistence.entity.ShipmentEntity;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingConstants;
import org.mapstruct.MappingTarget;

@Mapper(componentModel = MappingConstants.ComponentModel.SPRING)
public interface ShipmentMapper {

    ShipmentData toData(ShipmentEntity entity);

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    @Mapping(target = "logs", ignore = true)
    ShipmentEntity toEntity(ShipmentData data);

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    @Mapping(target = "logs", ignore = true)
    void updateEntity(@MappingTarget ShipmentEntity entity, ShipmentData data);

    default ShipmentStatus toStatus(String status) {
        return ShipmentStatus.fromValue(status);
    }

    default String fromStatus(ShipmentStatus status) {
        return status == null ? null : status.value();
    }
}
