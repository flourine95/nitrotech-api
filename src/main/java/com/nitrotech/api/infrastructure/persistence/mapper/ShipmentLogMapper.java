package com.nitrotech.api.infrastructure.persistence.mapper;

import com.nitrotech.api.domain.shipping.dto.ShipmentLogData;
import com.nitrotech.api.infrastructure.persistence.entity.ShipmentLogEntity;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingConstants;

@Mapper(componentModel = MappingConstants.ComponentModel.SPRING)
public interface ShipmentLogMapper {

    @Mapping(source = "shipment.id", target = "shipmentId")
    ShipmentLogData toData(ShipmentLogEntity entity);
}
